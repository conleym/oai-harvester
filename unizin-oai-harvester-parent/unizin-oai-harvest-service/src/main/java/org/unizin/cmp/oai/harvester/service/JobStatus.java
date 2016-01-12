package org.unizin.cmp.oai.harvester.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.sql.DataSource;

import org.skife.jdbi.v2.DBI;
import org.unizin.cmp.oai.ResumptionToken;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestStatistic;
import org.unizin.cmp.oai.harvester.job.JobNotification;
import org.unizin.cmp.oai.harvester.job.JobNotification.JobStatistic;

import com.fasterxml.jackson.annotation.JsonProperty;


public final class JobStatus {
    private final ConcurrentMap<String, Object> lastHarvestNotifications =
            new ConcurrentHashMap<>();

    @JsonProperty
    private volatile Map<String, Object> lastJobNotification;

    private final DBI dbi;

    JobStatus(final DataSource ds) {
        this.dbi = new DBI(ds);
    }

    private static String formatStackTrace(final Exception e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private static String formatInstant(final Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    private static void maybeAddLong(final Optional<Long> optional,
            final String key, final Map<String, Object> map) {
        if (optional.isPresent()) {
            map.put(key, optional.get());
        }
    }

    private static <T> T unwrapOptional(final Optional<T> optional) {
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

    void harvestUpdate(final HarvestNotification notification) {
        final Map<String, String> tags = notification.getTags();
        final String harvestName = tags.get("harvestName");
        final long harvestID = Long.valueOf(harvestName);
        final String started = formatInstant(notification.getStarted());
        final String ended = notification.getEnded().isPresent() ?
                formatInstant(notification.getEnded().get()) : null;

        final Map<String, Object> status = new HashMap<>();
        final ResumptionToken rt = notification.getResumptionToken();
        if (rt != null) {
            final Map<String, Object> resumptionToken = new HashMap<>();
            resumptionToken.put("token", rt.getToken());
            maybeAddLong(rt.getCompleteListSize(), "completeListSize",
                    resumptionToken);
            maybeAddLong(rt.getCursor(), "cursor", resumptionToken);
            final Optional<Instant> expirationDate = rt.getExpirationDate();
            if (expirationDate.isPresent()) {
                resumptionToken.put("expirationDate", formatInstant(
                        expirationDate.get()));
            }
            status.put("lastResumptionToken", resumptionToken);
        }
        status.put("tags", tags);
        status.put("statistics", notification.getStats());
        status.put("isRunning", notification.isRunning());
        status.put("isCancelled", notification.isCancelled());
        status.put("isInterrupted", notification.isInterrupted());
        status.put("isExplicitlyStopped", notification.isExplicitlyStopped());
        status.put("baseURI", notification.getBaseURI());
        status.put("verb", notification.getVerb());
        status.put("hasError", notification.hasError());
        status.put("started", started);
        if (ended != null) {
            status.put("ended", ended);
        }
        final Exception e = notification.getException();
        if (e != null) {
            status.put("exception", e);
        }
        status.put("lastRequestURI", notification.getLastRequestURI());

        // Update running harvest status.
        lastHarvestNotifications.put(harvestName, status);

        final URI uri = unwrapOptional(notification.getLastRequestURI());
        final String lastReqURI = uri == null ? null : uri.toString();
        final Map<String, String> params =
                unwrapOptional(notification.getLastRequestParameters());
        final String lastReqParams = params == null ? null : params.toString();
        try (final JobJDBI jdbi = dbi.open(JobJDBI.class)) {
            final String stackTrace = notification.hasError() ?
                    formatStackTrace(e) : null;
            jdbi.updateHarvest(harvestID, started, ended,
                    notification.getHarvestParameters().toString(),
                    notification.isCancelled(),
                    notification.isInterrupted(), lastReqURI, lastReqParams,
                    stackTrace,
                    notification.getStat(HarvestStatistic.REQUEST_COUNT),
                    notification.getStat(HarvestStatistic.RESPONSE_COUNT),
                    notification.getStat(HarvestStatistic.XML_EVENT_COUNT));
        }
    }

    void jobUpdate(final JobNotification notification) {
        final Map<String, Object> status = new HashMap<>();
        final String started = formatInstant(notification.getStarted());
        final String ended = (notification.getEnded().isPresent()) ?
                formatInstant(notification.getEnded().get()) : null;

        status.put("job", notification.getJobName());
        status.put("hasError", notification.hasError());
        final Exception e = notification.getException();
        if (e != null) {
            status.put("exception", notification.getException());
        }
        status.put("isRunning", notification.isRunning());
        status.put("statistics", notification.getStats());
        status.put("started", started);
        if (ended != null) {
            status.put("ended", ended);
        }
        status.put("harvests", lastHarvestNotifications);

        // Update running harvest status.
        lastJobNotification = status;

        // Update the database.
        try (final JobJDBI jdbi = dbi.open(JobJDBI.class)) {
            final String stackTrace = (notification.hasError()) ?
                    formatStackTrace(notification.getException()) : null;
            jdbi.updateJob(Long.valueOf(notification.getJobName()),
                    started, ended, stackTrace,
                    notification.getStat(JobStatistic.RECORDS_RECEIVED),
                    notification.getStat(JobStatistic.RECORD_BYTES_RECEIVED),
                    notification.getStat(JobStatistic.BATCHES_ATTEMPTED));
        }
    }
}
