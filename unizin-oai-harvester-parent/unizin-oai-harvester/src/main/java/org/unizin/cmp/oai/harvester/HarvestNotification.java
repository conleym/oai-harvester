package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.ResumptionToken;
import org.unizin.cmp.oai.harvester.Harvest.State;

/**
 * Immutable snapshot of harvest state.
 *
 */
public final class HarvestNotification {

    public static enum HarvestNotificationType {
        /** Notification type sent when a harvest is started. */
        HARVEST_STARTED,
        /**
         * Notification type sent when a harvest has ended, successfully or
         * not.
         */
        HARVEST_ENDED,
        /**
         * Notification sent when a valid HTTP response is received from the
         * server.
         * <p>
         * In particular, this notification is <em>not</em> sent when the
         * server sends an HTTP status code indicating an error of some kind.
         * In that case, the harvest simply ends with an error.
         */
        RESPONSE_RECEIVED,
        /**
         * Notification sent when a response from the server has been
         * processed, successfully or not.
         */
        RESPONSE_PROCESSED
    }

    /**
     * Names of statistics recorded by the harvester.
     *
     * @see HarvestNotification#getStats()
     * @see HarvestNotification#getStat(String)
     */
    public static final class Statistics {
        /** The number of requests sent so far during this harvest. */
        public static final String REQUEST_COUNT = "requestCount";
        /**
         * The number of valid responses received so far during this harvest.
         */
        public static final String RESPONSE_COUNT = "responseCount";
        /** The number of XML events parsed during this harvest. */
        public static final String XML_EVENT_COUNT = "xmlEventCount";

        /** No instances allowed. */
        private Statistics() {
        }
    }


    private final HarvestNotificationType type;
    private final boolean running;
    private final boolean explicitlyStopped;
    private final boolean cancelled;
    private final boolean interrupted;
    private final Exception exception;
    private final ResumptionToken resumptionToken;
    private final Instant lastResponseDate;
    private final HarvestParams params;
    private final Map<String, Long> stats;

    HarvestNotification(final HarvestNotificationType type,
            final State state, final Exception exception,
            final ResumptionToken resumptionToken,
            final Instant lastResponseDate,
            final HarvestParams params, final Map<String, Long> stats) {
        this.type = type;
        this.running = state.running;
        this.explicitlyStopped = state.explicitlyStopped;
        this.cancelled = state.cancelled;
        this.interrupted = state.interrupted;
        this.exception = exception;
        this.resumptionToken = resumptionToken;
        this.lastResponseDate = lastResponseDate;
        this.params = params;
        this.stats = Collections.unmodifiableMap(stats);
    }

    /**
     * Get the harvester stats that were current at the time this notification
     * was sent.
     *
     * @return an immutable map containing all the harvest stats.
     */
    public Map<String, Long> getStats() {
        return stats;
    }

    public Long getStat(final String stat) {
        return stats.get(stat);
    }

    public URI getBaseURI() {
        return params.getBaseURI();
    }

    public OAIVerb getVerb() {
        return params.getVerb();
    }

    public HarvestNotificationType getType() {
        return type;
    }

    public ResumptionToken getResumptionToken() {
        return resumptionToken;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isExplicitlyStopped() {
        return explicitlyStopped;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public boolean hasError() {
        return exception != null;
    }

    public Exception getException() {
        return exception;
    }

    public HarvestParams getHarvestParameters() {
        return params;
    }

    @Override
    public String toString() {
        return new StringBuilder(this.getClass().getName())
                .append("[type=").append(type)
                .append(", running=").append(running)
                .append(", explicitlyStopped=").append(explicitlyStopped)
                .append(", cancelled=").append(cancelled)
                .append(", interrupted=").append(interrupted)
                .append(", exception=").append(exception)
                .append(", resumptionToken=").append(resumptionToken)
                .append(", lastResponseDate=").append(lastResponseDate)
                .append(", stats=").append(stats).append("]")
                .toString();
    }
}
