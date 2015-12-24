package org.unizin.cmp.oai.harvester.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.Harvester;
import org.unizin.cmp.oai.harvester.job.HarvestJob;
import org.unizin.cmp.oai.harvester.job.JobNotification;

import com.fasterxml.jackson.annotation.JsonProperty;


public final class JobStatus {
    // TODO be less lazy. Grab what we want from the notifications.
    // TODO put what we want into the database, logging but not throwing exceptions.
    @JsonProperty
    private volatile JobNotification lastJobNotification;
    @JsonProperty
    private final ConcurrentMap<String, Object> lastHarvestNotifications =
            new ConcurrentHashMap<>();

    void harvestUpdate(final Harvester harvester,
            final HarvestNotification notification) {
        final Map<String, String> tags = notification.getTags();
        final String harvestName = tags.get("harvestName");
        lastHarvestNotifications.put(harvestName, notification);
    }

    void jobUpdate(final HarvestJob job,
            final JobNotification notification) {
        lastJobNotification = notification;
    }
}
