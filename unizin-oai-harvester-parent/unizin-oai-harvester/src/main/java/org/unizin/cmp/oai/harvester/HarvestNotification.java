package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

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
         * </p>
         */
        RESPONSE_RECEIVED,
        /**
         * Notification sent when a response from the server has been
         * processed, successfully or not.
         */
        RESPONSE_PROCESSED
    }


    public static enum HarvestStatistic {
        /** The number of requests sent so far during this harvest. */
        REQUEST_COUNT,
        /**
         * The number of valid responses received so far during this harvest.
         * <p>
         * There's no need to count invalid responses, as the first will stop
         * the harvest.
         * </p>
         */
        RESPONSE_COUNT,
        /** The number of XML events parsed during this harvest. */
        XML_EVENT_COUNT,
    }


    private final HarvestNotificationType type;
    private final Map<String, String> tags;
    private final boolean running;
    private final boolean explicitlyStopped;
    private final boolean cancelled;
    private final boolean interrupted;
    private final Optional<Exception> exception;
    private final Optional<ResumptionToken> resumptionToken;
    private final Optional<Instant> lastResponseDate;
    private final HarvestParams params;
    private final Map<HarvestStatistic, Long> stats;
    private final Optional<URI> lastRequestURI;
    private final Optional<SortedMap<String, String>> lastRequestParameters;
    private final Instant started;
    private final Optional<Instant> ended;

    HarvestNotification(final HarvestNotificationType type,
            final Map<String, String> tags, final State state,
            final Exception exception, final ResumptionToken resumptionToken,
            final Instant lastResponseDate, final HarvestParams params,
            final Map<HarvestStatistic, Long> stats, final URI lastRequestURI,
            final SortedMap<String, String> lastRequestParameters,
            final Instant started, final Instant ended) {
        this.type = type;
        this.tags = tags;
        this.running = state.running;
        this.explicitlyStopped = state.explicitlyStopped;
        this.cancelled = state.cancelled;
        this.interrupted = state.interrupted;
        this.exception = Optional.ofNullable(exception);
        this.resumptionToken = Optional.ofNullable(resumptionToken);
        this.lastResponseDate = Optional.ofNullable(lastResponseDate);
        this.params = params;
        this.stats = Collections.unmodifiableMap(stats);
        this.lastRequestURI = Optional.ofNullable(lastRequestURI);
        this.lastRequestParameters = lastRequestParameters == null ?
                Optional.empty() :
                Optional.of(Collections.unmodifiableSortedMap(
                        new TreeMap<>(lastRequestParameters)));
        this.started = started;
        this.ended = Optional.ofNullable(ended);
    }

    /**
     * Get the harvester stats that were current at the time this notification
     * was sent.
     *
     * @return an immutable map containing all the harvest stats.
     */
    public Map<HarvestStatistic, Long> getStats() {
        return stats;
    }

    public Long getStat(final HarvestStatistic stat) {
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

    public Optional<Instant> getLastReponseDate() {
        return lastResponseDate;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public String getTag(final String tag) {
        return tags.get(tag);
    }

    public Optional<ResumptionToken> getResumptionToken() {
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
        return exception.isPresent();
    }

    public Optional<Exception> getException() {
        return exception;
    }

    public HarvestParams getHarvestParameters() {
        return params;
    }

    public Optional<URI> getLastRequestURI() {
        return lastRequestURI;
    }

    public Optional<SortedMap<String, String>> getLastRequestParameters() {
        return lastRequestParameters;
    }

    public Instant getStarted() {
        return started;
    }

    public Optional<Instant> getEnded() {
        return ended;
    }

    @Override
    public String toString() {
        final String end = ended.isPresent() ? DateTimeFormatter.ISO_INSTANT
                .format(ended.get()) : "";
        return new StringBuilder(this.getClass().getName())
                .append("[type=").append(type)
                .append(", running=").append(running)
                .append(", explicitlyStopped=").append(explicitlyStopped)
                .append(", cancelled=").append(cancelled)
                .append(", interrupted=").append(interrupted)
                .append(", exception=").append(exception)
                .append(", resumptionToken=").append(resumptionToken)
                .append(", lastResponseDate=").append(lastResponseDate)
                .append(", lastRequestURI=").append(lastRequestURI)
                .append(", lastRequestParameters=")
                .append(lastRequestParameters)
                .append(", started=").append(DateTimeFormatter.ISO_INSTANT
                        .format(started))
                .append(", ended=").append(end)
                .append(", stats=").append(stats).append("]")
                .toString();
    }
}
