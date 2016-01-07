package org.unizin.cmp.oai.harvester.service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.unizin.cmp.oai.ResumptionToken;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.job.JobNotification;

import com.fasterxml.jackson.annotation.JsonProperty;


public final class JobStatus {
    private final ConcurrentMap<String, Object> lastHarvestNotifications =
            new ConcurrentHashMap<>();

    @JsonProperty
    private volatile Map<String, Object> lastJobNotification;


    private static String formatInstant(final Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    private static void maybeAddLong(final Optional<Long> optional,
            final String key, final Map<String, Object> map) {
        if (optional.isPresent()) {
            map.put(key, optional.get());
        }
    }

    void harvestUpdate(final HarvestNotification notification) {
        final Map<String, String> tags = notification.getTags();
        final String harvestName = tags.get("harvestName");
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
        status.put("started", formatInstant(notification.getStarted()));
        final Optional<Instant> ended = notification.getEnded();
        if (ended.isPresent()) {
            status.put("ended", formatInstant(ended.get()));
        }
        final Exception e = notification.getException();
        if (e != null) {
            status.put("exception", e);
        }
        status.put("lastRequestURI", notification.getLastRequestURI());
        lastHarvestNotifications.put(harvestName, status);
        updateDBHarvests(status);
    }

    private void updateDBHarvests(final Map<String, Object> status) {

    }

    void jobUpdate(final JobNotification notification) {
        final Map<String, Object> status = new HashMap<>();
        status.put("job", notification.getJobName());
        status.put("hasError", notification.hasError());
        final Exception e = notification.getException();
        if (e != null) {
            status.put("exception", notification.getException());
        }
        status.put("isRunning", notification.isRunning());
        status.put("statistics", notification.getStats());
        status.put("started", formatInstant(notification.getStarted()));
        final Optional<Instant> ended = notification.getEnded();
        if (ended.isPresent()) {
            status.put("ended", formatInstant(ended.get()));
        }
        status.put("harvests", lastHarvestNotifications);
        lastJobNotification = status;
        updateDBJobs(status);
    }

    private void updateDBJobs(final Map<String, Object> status) {

    }
}
