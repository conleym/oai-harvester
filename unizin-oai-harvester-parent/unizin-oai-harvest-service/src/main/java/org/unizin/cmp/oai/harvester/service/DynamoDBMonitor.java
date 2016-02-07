package org.unizin.cmp.oai.harvester.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DynamoDBMonitor implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            DynamoDBMonitor.class);

    private final DynamoDBClient client;
    private final JobManager jobManager;
    private final long increaseThreshold;
    private final long decreaseThreshold;
    private final long minWriteCapacity;
    private final long maxWriteCapacity;

    public DynamoDBMonitor(final DynamoDBClient client,
            final JobManager jobManager, final long increaseThreshold,
            final long decreaseThreshold, final long minWriteCapacity,
            final long maxWriteCapacity) {
        this.client = client;
        this.jobManager = jobManager;
        this.increaseThreshold = increaseThreshold;
        this.decreaseThreshold = decreaseThreshold;
        this.minWriteCapacity = minWriteCapacity;
        this.maxWriteCapacity = maxWriteCapacity;
    }

    @Override
    public void run() {
        final long maxQueueSize = jobManager.getMaxQueueSize();
        if (maxQueueSize >= increaseThreshold) {
            increaseWriteCapacity();
        } else if (maxQueueSize <= decreaseThreshold) {
            decreaseWriteCapacity();
        }
    }

    private void increaseWriteCapacity() {
        try {
            final long currentWriteCapacity = client.getWriteCapacity();
            if (currentWriteCapacity < maxWriteCapacity) {
                LOGGER.info("Increasing DynamoDB provisioned write capacity.");
                final long newWriteCapacity = Math.min(2 * currentWriteCapacity,
                        maxWriteCapacity);
                client.setWriteCapacity(newWriteCapacity);
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
            final long currentWriteCapacity = client.getWriteCapacity();
            if (currentWriteCapacity > minWriteCapacity) {
                LOGGER.info("Decreasing DynamoDB provisioned write capacity.");
                final long newWriteCapacity = Math.max(currentWriteCapacity / 2,
                        minWriteCapacity);
                client.setWriteCapacity(newWriteCapacity);
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
