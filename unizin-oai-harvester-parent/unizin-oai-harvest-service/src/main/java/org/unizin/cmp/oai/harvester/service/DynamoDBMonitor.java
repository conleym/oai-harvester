package org.unizin.cmp.oai.harvester.service;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputDescription;

public final class DynamoDBMonitor implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            DynamoDBMonitor.class);

    private final DynamoDBClient client;
    private final JobManager jobManager;
    private final long increaseThreshold;
    private final Duration increaseCooldownInterval;
    private final long decreaseThreshold;
    private final Duration decreaseCooldownInterval;
    private final long minWriteCapacity;
    private final long maxWriteCapacity;
    private Instant lastIncrease;
    private Instant lastDecrease;


    public DynamoDBMonitor(final DynamoDBClient client,
            final JobManager jobManager, final long increaseThreshold,
            final Duration increaseCooldownInterval,
            final long decreaseThreshold,
            final Duration decreaseCooldownInterval,
            final long minWriteCapacity, final long maxWriteCapacity) {
        this.client = client;
        this.jobManager = jobManager;
        this.increaseThreshold = increaseThreshold;
        this.increaseCooldownInterval = increaseCooldownInterval;
        this.decreaseThreshold = decreaseThreshold;
        this.decreaseCooldownInterval = decreaseCooldownInterval;
        this.minWriteCapacity = minWriteCapacity;
        this.maxWriteCapacity = maxWriteCapacity;
        // Prevent immediate decreases on startup.
        lastDecrease = Instant.now();
        // Allow immediate increases.
        lastIncrease = Instant.EPOCH;
    }

    @Override
    public void run() {
        final long maxQueueSize = jobManager.getMaxQueueSize();
        final Instant now = Instant.now();
        if (maxQueueSize >= increaseThreshold) {
            if (Duration.between(lastIncrease, now)
                    .compareTo(increaseCooldownInterval) > 0) {
                increaseWriteCapacity();
                lastIncrease = now;
            }
        } else if (maxQueueSize <= decreaseThreshold) {
            final int cmpDecrease = Duration.between(lastDecrease, now)
                    .compareTo(decreaseCooldownInterval);
            final int cmpIncrease = Duration.between(lastIncrease, now)
                    .compareTo(decreaseCooldownInterval);
            if (cmpDecrease > 0 && cmpIncrease > 0) {
                decreaseWriteCapacity();
                lastDecrease = now;
            }
        }
    }

    private void increaseWriteCapacity() {
        try {
            final ProvisionedThroughputDescription throughput =
                    client.getThroughputDescription();
            final long currentWriteCapacity =
                    throughput.getWriteCapacityUnits();
            if (currentWriteCapacity < maxWriteCapacity) {
                LOGGER.info("Increasing DynamoDB provisioned write capacity.");
                final long newWriteCapacity = Math.min(2 * currentWriteCapacity,
                        maxWriteCapacity);
                client.setThroughput(throughput.getReadCapacityUnits(),
                        newWriteCapacity);
                LOGGER.info("Increased write capacity from {} to {}.",
                        currentWriteCapacity, newWriteCapacity);
            } else {
                LOGGER.info("Cannot increase DynamoDB write capacity " +
                        " because it's already at the maximum: {}",
                        maxWriteCapacity);
            }
        } catch (final Exception e) {
            LOGGER.error("Error increasing DynamoDB capacity.", e);
        }
    }

    private void decreaseWriteCapacity() {
        try {
            final ProvisionedThroughputDescription throughput =
                    client.getThroughputDescription();
            final long currentWriteCapacity =
                    throughput.getWriteCapacityUnits();
            if (currentWriteCapacity > minWriteCapacity) {
                LOGGER.info("Decreasing DynamoDB provisioned write capacity.");
                final long newWriteCapacity = Math.max(currentWriteCapacity / 2,
                        minWriteCapacity);
                client.setThroughput(throughput.getReadCapacityUnits(),
                        newWriteCapacity);
                LOGGER.info("Decreased write capacity from {} to {}.",
                        currentWriteCapacity, newWriteCapacity);
            } else {
                LOGGER.info("Cannot decrease DynamoDB write capacity " +
                        " because it's already at the minimum: {}",
                        minWriteCapacity);
            }
        } catch (final Exception e) {
            LOGGER.error("Error decreasing DynamoDB capacity.", e);
        }
    }
}
