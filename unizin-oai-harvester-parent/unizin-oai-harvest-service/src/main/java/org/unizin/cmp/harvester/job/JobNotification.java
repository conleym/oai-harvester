package org.unizin.cmp.harvester.job;

import java.util.Collections;
import java.util.Map;


public final class JobNotification {
    public static enum JobNotificationType {
        STARTED,
        STOPPED,
        BATCH_STARTED,
        BATCH_FINISHED
    }


    public static enum JobStatistic {
        /**
         * The number of records received by the consumer thread on the blocking
         * queue so far.
         */
        RECORDS_RECEIVED,
        /**
         * The number of batch writes to DynamoDB attempted so far.
         */
        BATCHES_ATTEMPTED,
        /**
         * The current queue size.
         */
        QUEUE_SIZE
    }


    private final JobNotificationType type;
    private final boolean running;
    private final Map<JobStatistic, Long> stats;
    private final Exception exception;


    JobNotification(final JobNotificationType type,
            final boolean running,
            final Map<JobStatistic, Long> stats,
            final Exception exception) {
        this.type = type;
        this.running = running;
        this.stats = Collections.unmodifiableMap(stats);
        this.exception = exception;
    }

    public JobNotificationType getType() { return type; }
    public boolean isRunning() { return running; }
    public Map<JobStatistic, Long> getStats() { return stats; }
    public Exception getException() { return exception; }
}
