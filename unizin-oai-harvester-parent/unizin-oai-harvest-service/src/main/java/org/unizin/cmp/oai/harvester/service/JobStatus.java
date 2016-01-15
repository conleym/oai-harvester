package org.unizin.cmp.oai.harvester.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.skife.jdbi.v2.DBI;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.ResumptionToken;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestStatistic;
import org.unizin.cmp.oai.harvester.job.JobNotification;
import org.unizin.cmp.oai.harvester.job.JobNotification.JobStatistic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;


@JsonIgnoreProperties(ignoreUnknown=true)
public final class JobStatus {
    private final ConcurrentMap<String, Object> lastHarvestNotifications =
            new ConcurrentHashMap<>();

    @JsonProperty
    private volatile Map<String, Object> lastJobNotification;

    private final DBI dbi;

    JobStatus(final DBI dbi) {
        this.dbi = dbi;
    }

    public boolean loadFromDB(final long jobID) {
        final String jobName = String.valueOf(jobID);
        try (final JobJDBI jdbi = dbi.open(JobJDBI.class)) {
            final List<Map<String, Object>> result = jdbi.findJobByID(jobID);
            boolean found = !result.isEmpty();
            if (found) {
                final Map<String, Object> first = result.get(0);
                final String jobStarted = formatInstant(
                        (Instant)first.get("JOB_START"));
                final Instant ended = (Instant)first.get("JOB_END");
                final String jobStackTrace =
                        (String)first.get("JOB_STACK_TRACE");
                final Map<JobStatistic, Long> jobStats = new TreeMap<>();
                jobStats.put(JobStatistic.RECORDS_RECEIVED,
                        (Long)first.get("JOB_RECORDS_RECEIVED"));
                jobStats.put(JobStatistic.RECORD_BYTES_RECEIVED,
                        (Long)first.get("JOB_RECORD_BYTES_RECEIVED"));
                jobStats.put(JobStatistic.BATCHES_ATTEMPTED,
                        (Long)first.get("JOB_BATCHES_ATTEMPTED"));
                final Map<String, Object> status = jobStatusMap(jobName,
                        jobStarted, formatInstant(ended), false,
                        (jobStackTrace != null), jobStackTrace, jobStats);
                final Map<String, Object> harvests = new TreeMap<>();
                result.forEach(x -> {
                    final String harvestName =
                            String.valueOf(x.get("HARVEST_ID"));
                    final String harvestStarted = formatInstant(
                            (Instant)x.get("HARVEST_STARTED"));
                    final String harvestEnded = formatInstant(
                            (Instant)x.get("HARVEST_ENDED"));
                    final String harvestStackTrace = (String)x.get(
                            "HARVEST_STACK_TRACE");
                    final Map<HarvestStatistic, Long> harvestStats =
                            new TreeMap<>();
                    harvestStats.put(HarvestStatistic.REQUEST_COUNT,
                            (Long)x.get("HARVEST_REQUEST_COUNT"));
                    harvestStats.put(HarvestStatistic.RESPONSE_COUNT,
                            (Long)x.get("HARVEST_RESPONSE_COUNT"));
                    harvestStats.put(HarvestStatistic.XML_EVENT_COUNT,
                            (Long)x.get("HARVEST_XML_EVENT_COUNT"));
                    final Map<String, Object> harvest = harvestStatusMap(
                            harvestStarted, harvestEnded, false,
                            (Boolean)x.get("HARVEST_CANCELLED"),
                            (Boolean)x.get("HARVEST_INTERRUPTED"),
                            (Boolean)x.get("HARVEST_EXPLICITLY_STOPPED"),
                            (harvestStackTrace != null), harvestStackTrace,
                            (String)x.get("REPOSITORY_BASE_URI"),
                            OAIVerb.valueOf((String)x.get("HARVEST_VERB")),
                            (String)x.get("HARVEST_LAST_REQUEST_URI"),
                            (String)x.get("HARVEST_LAST_REQUEST_PARAMETERS"),
                            null, harvestStats, Optional.ofNullable(
                                    (Instant)x.get(
                                            "HARVEST_LAST_RESPONSE_DATE")),
                            null, Optional.empty(), Optional.empty(),
                            Optional.empty());
                    harvests.put(harvestName, harvest);
                });
                status.put("harvests", harvests);
                lastJobNotification = ImmutableMap.of(jobName, status);
            }
            return found;
        }
    }

    private static String formatStackTrace(final Optional<Exception> e) {
        if (e.isPresent()) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            e.get().printStackTrace(pw);
            return sw.toString();
        }
        return null;
    }

    private static String formatInstant(final Instant instant) {
        if (instant == null) {
            return null;
        }
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    private static String formatInstant(final Optional<Instant> instant) {
        if (instant.isPresent()) {
            return DateTimeFormatter.ISO_INSTANT.format(instant.get());
        }
        return null;
    }

    private static <T> void maybeAdd(final Optional<T> optional,
            final String key, final Map<String, Object> map) {
        if (optional.isPresent()) {
            map.put(key, optional.get());
        }
    }

    private static void maybeAddObject(final Object object, final String key,
            final Map<String, Object> map) {
        if (object != null) {
            map.put(key, object);
        }
    }

    private static <T> T unwrapOptional(final Optional<T> optional) {
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

    private static Map<String, Object> harvestStatusMap(
            final String harvestStarted, final String harvestEnded,
            final boolean isRunning, final boolean isCancelled,
            final boolean isInterrupted, final boolean isExplicitlyStopped,
            final boolean hasError, final String stackTrace,
            final String baseURI, final OAIVerb verb,
            final String lastRequestURI,
            final String lastRequestParams,
            final Map<String, String> tags,
            final Map<HarvestStatistic, Long> stats,
            final Optional<Instant> lastResponseDate,
            final String resumptionToken, final Optional<Long> completeListSize,
            final Optional<Long> cursor,
            final Optional<Instant> expirationDate) {
        final Map<String, Object> status = new TreeMap<>();
        status.put("statistics", stats);
        status.put("isRunning", isRunning);
        status.put("isCancelled", isCancelled);
        status.put("isInterrupted", isInterrupted);
        status.put("isExplicitlyStopped", isExplicitlyStopped);
        status.put("baseURI", baseURI);
        status.put("verb", verb);
        maybeAddObject(tags, "tags", status);
        maybeAddObject(stackTrace, "exception", status);
        maybeAddObject(lastRequestParams, "lastRequestParameters", status);
        maybeAddObject(lastRequestURI, "lastRequestURI", status);
        maybeAdd(lastResponseDate, "lastResponseDate", status);

        final Map<String, Object> rt = new TreeMap<>();
        maybeAddObject(resumptionToken, "token", rt);
        maybeAdd(completeListSize, "completeListSize", rt);
        maybeAdd(cursor, "cursor", rt);
        if (expirationDate.isPresent()) {
            rt.put("expirationDate", formatInstant(expirationDate.get()));
        }
        if (! rt.isEmpty()) {
            status.put("lastResumptionToken", rt);
        }
        return status;
    }

    void harvestUpdate(final HarvestNotification notification) {
        final Map<String, String> tags = notification.getTags();
        final String harvestName = tags.get("harvestName");
        final long harvestID = Long.valueOf(harvestName);
        final Optional<ResumptionToken> rt = notification.getResumptionToken();
        String token = null;
        Optional<Long> completeListSize = Optional.empty();
        Optional<Long> cursor = Optional.empty();
        Optional<Instant> expirationDate = Optional.empty();
        if (rt.isPresent()) {
            token = rt.get().getToken();
            completeListSize = rt.get().getCompleteListSize();
            cursor = rt.get().getCursor();
            expirationDate = rt.get().getExpirationDate();
        }
        final String started = formatInstant(notification.getStarted());
        final String ended = formatInstant(notification.getEnded());
        final String stackTrace = formatStackTrace(notification.getException());
        final String lastRequestURI = notification.getLastRequestURI()
                .isPresent() ? notification.getLastRequestURI().get().toString()
                        : null;
        final String lastRequestParameters = notification
                .getLastRequestParameters().isPresent() ? notification
                        .getLastRequestParameters().get().toString() : null;
        final Map<String, Object> status = harvestStatusMap(started, ended,
                notification.isRunning(), notification.isCancelled(),
                notification.isInterrupted(),
                notification.isExplicitlyStopped(),
                notification.hasError(), stackTrace,
                notification.getBaseURI().toString(),
                notification.getVerb(), lastRequestURI, lastRequestParameters,
                tags, notification.getStats(),
                notification.getLastReponseDate(), token, completeListSize,
                cursor, expirationDate);
        // Update running harvest status.
        lastHarvestNotifications.put(harvestName, status);
        final Map<String, String> params =
                unwrapOptional(notification.getLastRequestParameters());
        final String lastReqParams = params == null ? null : params.toString();
        try (final JobJDBI jdbi = dbi.open(JobJDBI.class)) {
            jdbi.updateHarvest(harvestID, started, ended,
                    notification.getHarvestParameters().toString(),
                    notification.isCancelled(),
                    notification.isInterrupted(), lastRequestURI,
                    lastReqParams, stackTrace,
                    notification.getStat(HarvestStatistic.REQUEST_COUNT),
                    notification.getStat(HarvestStatistic.RESPONSE_COUNT),
                    notification.getStat(HarvestStatistic.XML_EVENT_COUNT));
        }
    }

    private static Map<String, Object> jobStatusMap(final String jobName,
            final String jobStarted, final String jobEnded,
            final boolean isRunning, final boolean hasError,
            final String stackTrace, final Map<JobStatistic, Long> stats) {
        final Map<String, Object> status = new TreeMap<>();
        status.put("hasError", hasError);
        maybeAddObject(stackTrace, "exception", status);
        status.put("isRunning", isRunning);
        status.put("statistics", stats);
        status.put("started", jobStarted);
        maybeAddObject(jobEnded, "ended", status);
        return status;
    }

    void jobUpdate(final JobNotification notification) {
        final String started = formatInstant(notification.getStarted());
        final String ended = (notification.getEnded().isPresent()) ?
                formatInstant(notification.getEnded().get()) : null;
        final String stackTrace =
                formatStackTrace(notification.getException());
        final Map<String, Object> status = jobStatusMap(
                notification.getJobName(), started, ended,
                notification.isRunning(), notification.hasError(), stackTrace,
                notification.getStats());
        status.put("harvests", lastHarvestNotifications);

        // Update running harvest status.
        lastJobNotification = status;

        // Update the database.
        try (final JobJDBI jdbi = dbi.open(JobJDBI.class)) {
            jdbi.updateJob(Long.valueOf(notification.getJobName()),
                    started, ended, stackTrace,
                    notification.getStat(JobStatistic.RECORDS_RECEIVED),
                    notification.getStat(JobStatistic.RECORD_BYTES_RECEIVED),
                    notification.getStat(JobStatistic.BATCHES_ATTEMPTED));
        }
    }
}
