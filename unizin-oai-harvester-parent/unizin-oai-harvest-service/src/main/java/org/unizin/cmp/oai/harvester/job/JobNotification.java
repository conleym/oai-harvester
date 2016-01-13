package org.unizin.cmp.oai.harvester.job;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;


public final class JobNotification {
    public static enum JobNotificationType {
        STARTED,
        STOPPED,
        BATCH_STARTED,
        BATCH_FINISHED
    }

    public static enum JobStatistic {
        /**
         * The number of records received by the consumer thread so far.
         */
        RECORDS_RECEIVED,
        /**
         * The number of bytes in the XML of records received by the consumer
         * thread so far.
         */
        RECORD_BYTES_RECEIVED,
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
    private final Optional<Instant> ended;


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
        this.ended = Optional.ofNullable(ended);
    }

    public JobNotificationType getType() { return type; }
    public String getJobName() { return jobName; }
    public boolean isRunning() { return running; }
    public Map<JobStatistic, Long> getStats() { return stats; }
    public Long getStat(final JobStatistic stat) { return stats.get(stat); }
    public Exception getException() { return exception; }
    public boolean hasError() { return exception != null; }
    public Instant getStarted() { return started; }
    public Optional<Instant> getEnded() { return ended; }
}
