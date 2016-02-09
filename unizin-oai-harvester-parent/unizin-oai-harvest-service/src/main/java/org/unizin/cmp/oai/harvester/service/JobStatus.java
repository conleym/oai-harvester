package org.unizin.cmp.oai.harvester.service;

import static org.unizin.cmp.oai.harvester.service.Status.addIfPresent;
import static org.unizin.cmp.oai.harvester.service.Status.convertResumptionToken;
import static org.unizin.cmp.oai.harvester.service.Status.formatHeaders;
import static org.unizin.cmp.oai.harvester.service.Status.formatInstant;
import static org.unizin.cmp.oai.harvester.service.Status.formatMap;
import static org.unizin.cmp.oai.harvester.service.Status.formatStackTrace;
import static org.unizin.cmp.oai.harvester.service.Status.formatURI;

import java.sql.Blob;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.OAIError;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestStatistic;
import org.unizin.cmp.oai.harvester.exception.HarvesterHTTPStatusException;
import org.unizin.cmp.oai.harvester.exception.OAIProtocolException;
import org.unizin.cmp.oai.harvester.job.JobNotification;
import org.unizin.cmp.oai.harvester.job.JobNotification.JobStatistic;
import org.unizin.cmp.oai.harvester.service.db.DBIUtils;
import org.unizin.cmp.oai.harvester.service.db.JobJDBI;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

/**
 * Status information for a single job.
 * <p>
 * Instances are responsible for reading and writing status information to and
 * from the database.
 * </p>
 */
public final class JobStatus {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            JobStatus.class);
    private static final String[] EMPTY_STRING_ARRAY = new String[]{};

    private final ConcurrentMap<String, Object> lastHarvestNotifications =
            new ConcurrentHashMap<>();

    @JsonProperty
    private volatile Map<String, Object> lastJobNotification;
    private volatile Long queueSize;
    private final DBI dbi;

    JobStatus(final DBI dbi) {
        this.dbi = dbi;
    }

    private static <T> List<T> toList(final T[] array) {
        if (array == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(array);
    }

    /**
     * Load status information from the database rows of a single job.
     *
     * @param result
     *            the mapped database rows of a single job.
     * @return {@code true} iff the list was nonempty.
     */
    public boolean loadFromResults(final List<Map<String, Object>> result) {
        if (result.isEmpty()) {
            return false;
        }
        final Map<String, Object> first = result.get(0);
        final String jobName = String.valueOf(first.get("JOB_ID"));
        final Instant jobStarted = (Instant)first.get("JOB_START");
        final Optional<Instant> ended = Optional.ofNullable(
                (Instant)first.get("JOB_END"));
        final Optional<String> jobStackTrace = Optional.ofNullable(
                (String)first.get("JOB_STACK_TRACE"));
        final Map<JobStatistic, Long> jobStats = new TreeMap<>();
        jobStats.put(JobStatistic.RECORDS_RECEIVED,
                (Long)first.get("JOB_RECORDS_RECEIVED"));
        jobStats.put(JobStatistic.RECORD_BYTES_RECEIVED,
                (Long)first.get("JOB_RECORD_BYTES_RECEIVED"));
        jobStats.put(JobStatistic.BATCHES_ATTEMPTED,
                (Long)first.get("JOB_BATCHES_ATTEMPTED"));
        final Map<String, Object> status = jobStatusMap(jobName,
                jobStarted, ended, false,
                jobStackTrace.isPresent(), jobStackTrace, jobStats);
        final Map<String, Object> harvests = new TreeMap<>();
        result.forEach(x -> {
            final String harvestName = String.valueOf(x.get("HARVEST_ID"));
            final Instant harvestStarted = (Instant)x.get("HARVEST_START");
            final Optional<Instant> harvestEnded = Optional.ofNullable(
                    (Instant)x.get("HARVEST_END"));
            final Optional<String> harvestStackTrace = Optional.ofNullable(
                    (String)x.get("HARVEST_STACK_TRACE"));
            final Map<HarvestStatistic, Long> harvestStats = new TreeMap<>();
            harvestStats.put(HarvestStatistic.REQUEST_COUNT,
                    (Long)x.get("HARVEST_REQUEST_COUNT"));
            harvestStats.put(HarvestStatistic.RESPONSE_COUNT,
                    (Long)x.get("HARVEST_RESPONSE_COUNT"));
            harvestStats.put(HarvestStatistic.XML_EVENT_COUNT,
                    (Long)x.get("HARVEST_XML_EVENT_COUNT"));
            @SuppressWarnings("unchecked")
            final List<OAIError> protocolErrors =
                    (List<OAIError>)x.get("HARVEST_PROTOCOL_ERRORS");
            final Optional<String> httpErrorContentEncoding =
                    Optional.ofNullable(
                            (String)x.get(
                                    "HARVEST_HTTP_ERROR_CONTENT_ENCODING"));
            final Optional<String> httpErrorContentType =
                    Optional.ofNullable(
                            (String)x.get("HARVEST_HTTP_ERROR_CONTENT_TYPE"));
            final Optional<Blob> responseBody = Optional.ofNullable(
                    (Blob)x.get("HARVEST_HTTP_ERROR_RESPONSE_BODY"));
            final Map<String, Object> harvest = harvestStatusMap(harvestStarted,
                    harvestEnded, false, (Boolean)x.get("HARVEST_CANCELLED"),
                    (Boolean)x.get("HARVEST_INTERRUPTED"),
                    (Boolean)x.get("HARVEST_EXPLICITLY_STOPPED"),
                    harvestStackTrace.isPresent(), harvestStackTrace,
                    (String)x.get("REPOSITORY_BASE_URI"),
                    OAIVerb.valueOf((String)x.get("HARVEST_VERB")),
                    (String)x.get("HARVEST_INITIAL_PARAMETERS"),
                    Optional.ofNullable((String)x.get(
                            "HARVEST_LAST_REQUEST_URI")),
                    Optional.ofNullable((String)x.get(
                            "HARVEST_LAST_REQUEST_PARAMETERS")),
                    null, harvestStats,
                    Optional.ofNullable((Instant)x.get(
                            "HARVEST_LAST_ESPONSE_DATE")),
                    Optional.empty(),
                    Optional.ofNullable(
                            (Integer)x.get("HARVEST_HTTP_ERROR_STATUS_CODE")),
                    toList((Object[])x.get("HARVEST_HTTP_ERROR_HEADERS")),
                    responseBody, httpErrorContentEncoding,
                    httpErrorContentType, protocolErrors);

            harvests.put(harvestName, harvest);
        });
        status.put("harvests", harvests);
        lastJobNotification = ImmutableMap.of(jobName, status);
        return true;
    }

    public boolean loadFromDB(final long jobID) {
        try (final JobJDBI jdbi = DBIUtils.jobDBI(dbi)) {
            return loadFromResults(jdbi.findJobByID(jobID));
        }
    }

    private static Map<String, Object> harvestStatusMap(
            final Instant harvestStarted, final Optional<Instant> harvestEnded,
            final boolean isRunning, final boolean isCancelled,
            final boolean isInterrupted, final boolean isExplicitlyStopped,
            final boolean hasError, final Optional<String> stackTrace,
            final String baseURI, final OAIVerb verb,
            final String initialParameters,
            final Optional<String> lastRequestURI,
            final Optional<String> lastRequestParams,
            final Map<String, String> tags,
            final Map<HarvestStatistic, Long> statistics,
            final Optional<Instant> lastResponseDate,
            final Optional<Map<String, Object>> resumptionToken,
            final Optional<Integer> httpErrorStatus,
            final List<?> httpErrorHeaders,
            final Optional<Blob> httpErrorResponseBody,
            final Optional<String> httpErrorResponseBodyEncoding,
            final Optional<String> httpErrorResponseBodyContentType,
            final List<OAIError> oaiErrors) {
        final Map<String, Object> status = new TreeMap<>();
        status.put("started", formatInstant(harvestStarted));
        addIfPresent(formatInstant(harvestEnded), "ended", status);
        status.put("isRunning", isRunning);
        status.put("isCancelled", isCancelled);
        status.put("isInterrupted", isInterrupted);
        status.put("isExplicitlyStopped", isExplicitlyStopped);
        status.put("hasError", hasError);
        addIfPresent(stackTrace, "exception", status);
        status.put("baseURI", baseURI);
        status.put("verb", verb);
        status.put("firstRequestParams", initialParameters);
        addIfPresent(lastRequestURI, "lastRequestURI", status);
        addIfPresent(lastRequestParams, "lastRequestParameters", status);
        if (tags != null) {
            status.put("tags", tags);
        }
        status.put("statistics", statistics);
        addIfPresent(formatInstant(lastResponseDate), "lastResponseDate",
                status);
        addIfPresent(resumptionToken, "lastResumptionToken", status);
        if (httpErrorStatus.isPresent()) {
            final Map<String, Object> map = new TreeMap<>();
            map.put("statusCode", httpErrorStatus.get());
            if (! httpErrorHeaders.isEmpty()) {
                map.put("headers", httpErrorHeaders);
            }
            addIfPresent(httpErrorResponseBodyContentType,
                    "responseBodyContentType", map);
            // Clients should probably assume ISO-8859-1 if absent.
            addIfPresent(httpErrorResponseBodyEncoding, "responseBodyEncoding",
                    map);
            addIfPresent(httpErrorResponseBody, "responseBody", map);
            status.put("httpError", map);
        }
        if (! oaiErrors.isEmpty()) {
            status.put("oaiProtocolErrors", oaiErrors);
        }
        return status;
    }

    void harvestUpdate(final HarvestNotification notification) {
        final Map<String, String> tags = notification.getTags();
        final String harvestName = tags.get("harvestName");
        final long harvestID = Long.valueOf(harvestName);
        final Optional<String> lastRequestURI = formatURI(
                notification.getLastRequestURI());
        final Optional<String> lastRequestParameters = formatMap(
                notification.getLastRequestParameters());
        Optional<String> stackTrace = formatStackTrace(
                notification.getException());
        Optional<Integer> httpStatus = Optional.empty();
        String[] httpHeaders = EMPTY_STRING_ARRAY;
        List<OAIError> oaiErrors = Collections.emptyList();
        if (notification.getException().isPresent()) {
            final Exception ex = notification.getException().get();
            if (ex instanceof HarvesterHTTPStatusException) {
                final HarvesterHTTPStatusException hhse =
                        (HarvesterHTTPStatusException)ex;
                httpStatus = Optional.of(hhse.getStatusLine().getStatusCode());
                httpHeaders = formatHeaders(hhse.getHeaders());
            } else if (ex instanceof OAIProtocolException) {
                oaiErrors = ((OAIProtocolException)ex)
                        .getOAIErrors();
            }
        }
        final String initialParameters = notification.getHarvestParameters()
                .getParameters().toString();
        /*
         * The response body can be read reliably only once, so it will always
         * be empty while the harvest is running (we need use that read to write
         * it to the database). It can be read only from the database.
         */
        final Optional<Blob> httpErrorResponseBody = Optional.empty();
        final Map<String, Object> status = harvestStatusMap(
                notification.getStarted(), notification.getEnded(),
                notification.isRunning(), notification.isCancelled(),
                notification.isInterrupted(),
                notification.isExplicitlyStopped(),
                notification.hasError(), stackTrace,
                notification.getBaseURI().toString(),
                notification.getVerb(), initialParameters, lastRequestURI,
                lastRequestParameters, tags, notification.getStats(),
                notification.getLastReponseDate(),
                convertResumptionToken(notification.getResumptionToken()),
                httpStatus, Arrays.asList(httpHeaders), httpErrorResponseBody,
                Optional.empty(), Optional.empty(), oaiErrors);
        // Update running harvest status.
        lastHarvestNotifications.put(harvestName, status);
        try (final JobJDBI jdbi = DBIUtils.jobDBI(dbi)) {
            jdbi.harvestDatabaseUpdate(harvestID, lastRequestURI,
                    lastRequestParameters, stackTrace, notification, LOGGER);
        } catch (final Exception e) {
            LOGGER.error("Error updating harvest status database.", e);
        }
    }

    private static Map<String, Object> jobStatusMap(final String jobName,
            final Instant jobStarted, final Optional<Instant> jobEnded,
            final boolean isRunning, final boolean hasError,
            final Optional<String> stackTrace,
            final Map<JobStatistic, Long> stats) {
        final Map<String, Object> status = new TreeMap<>();
        status.put("hasError", hasError);
        addIfPresent(stackTrace, "exception", status);
        status.put("isRunning", isRunning);
        status.put("statistics", stats);
        status.put("started", formatInstant(jobStarted));
        addIfPresent(formatInstant(jobEnded), "ended", status);
        return status;
    }

    void jobUpdate(final JobNotification notification) {
        queueSize = notification.getStat(JobStatistic.QUEUE_SIZE);
        final Optional<String> stackTrace = formatStackTrace(
                notification.getException());
        final Map<String, Object> status = jobStatusMap(
                notification.getJobName(), notification.getStarted(),
                notification.getEnded(), notification.isRunning(),
                notification.hasError(), stackTrace, notification.getStats());
        status.put("harvests", lastHarvestNotifications);

        // Update running harvest status.
        lastJobNotification = status;

        // Update the database.
        try (final JobJDBI jdbi = DBIUtils.jobDBI(dbi)) {
            jdbi.updateJob(Long.valueOf(notification.getJobName()),
                    notification.getStarted(), notification.getEnded(),
                    stackTrace,
                    notification.getStat(JobStatistic.RECORDS_RECEIVED),
                    notification.getStat(JobStatistic.RECORD_BYTES_RECEIVED),
                    notification.getStat(JobStatistic.BATCHES_ATTEMPTED));
        } catch (final Exception e) {
            LOGGER.error("Error updating job status database.", e);
        }
    }

    long getQueueSize() {
        return queueSize == null ? 0 : queueSize;
    }
}
