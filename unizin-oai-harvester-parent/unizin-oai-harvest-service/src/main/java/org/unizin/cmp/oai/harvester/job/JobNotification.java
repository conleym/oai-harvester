package org.unizin.cmp.oai.harvester.job;

import java.time.Instant;
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
    private final String jobName;
    private final boolean running;
    private final Map<JobStatistic, Long> stats;
    private final Exception exception;
    private final Instant started;
    private final Instant ended;


    JobNotification(final JobNotificationType type,
            final String jobName, final boolean running,
            final Map<JobStatistic, Long> stats,
            final Exception exception, final Instant started,
            final Instant ended) {
        this.type = type;
        this.jobName = jobName;
        this.running = running;
        this.stats = Collections.unmodifiableMap(stats);
        this.exception = exception;
        this.started = started;
        this.ended = ended;
    }

    public JobNotificationType getType() { return type; }
    public String getJobName() { return jobName; }
    public boolean isRunning() { return running; }
    public Map<JobStatistic, Long> getStats() { return stats; }
    public Exception getException() { return exception; }
    public Instant getStarted() { return started; }
    public Instant getEnded() { return ended; }
}
