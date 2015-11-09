package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.unizin.cmp.oai.OAIRequestParameter;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.ResumptionToken;

/**
 * Immutable snapshot of harvest state.
 *
 */
public final class HarvestNotification {

    public static enum HarvestNotificationType {
        HARVEST_STARTED,
        HARVEST_ENDED,
        RESPONSE_RECEIVED,
        RESPONSE_PROCESSED
    }

    /**
     * Names of statistics recorded by the harvester.
     * @see HarvestNotification#getStats()
     * @see HarvestNotification#getStat(String)
     */
    public static final class Statistics {
        /** The number of requests sent so far during this harvest. */
        public static final String REQUEST_COUNT = "requestCount";
        /** The number of responses received so far during this harvest. */
        public static final String RESPONSE_COUNT = "responseCount";

        /** No instances allowed. */
        private Statistics() {}
    }

    /** Gives read-only access to the enclosed {@link HarvestParams}. */
    public static final class NotificationParams {
        private final Map<String, String> paramMap;
        private NotificationParams(final Map<String, String> m) {
            this.paramMap = m;
        }
        public String get(final OAIRequestParameter param) {
            return paramMap.get(param.paramName());
        }

        public String get(final String paramName) {
            return paramMap.get(paramName);
        }

        @Override
        public String toString() {
            return paramMap.toString();
        }
    }

    private final HarvestNotificationType type;
    private final boolean isStarted;
    private final boolean hasError;
    private final boolean isStoppedByUser;
    private final ResumptionToken resumptionToken;
    private final Instant lastResponseDate;
    private final HarvestParams params;
    private final NotificationParams notificationParams;
    private final Map<String, Long> stats;

    public HarvestNotification(final HarvestNotificationType type,
            final boolean isStarted, final boolean hasError,
            final boolean isStoppedByUser,
            final ResumptionToken resumptionToken,
            final Instant lastResponseDate, final HarvestParams params,
            final Map<String, Long> stats) {
        this.type = type;
        this.isStarted = isStarted;
        this.hasError = hasError;
        this.isStoppedByUser = isStoppedByUser;
        this.resumptionToken = resumptionToken;
        this.lastResponseDate = lastResponseDate;
        this.params = params;
        this.notificationParams = new NotificationParams(
                Collections.unmodifiableMap(params.getParameters()));
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

    public boolean isStoppedByUser() {
        return isStoppedByUser;
    }

    public boolean hasError() {
        return hasError;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public NotificationParams getHarvestParameters() {
        return notificationParams;
    }

    @Override
    public String toString() {
        return new StringBuilder(this.getClass().getName())
                .append("[type=")
                .append(type)
                .append(", isStarted=")
                .append(isStarted)
                .append(", hasError=")
                .append(hasError)
                .append(", stoppedByUser=")
                .append(isStoppedByUser)
                .append(", resumptionToken=")
                .append(resumptionToken)
                .append(", lastResponseDate=")
                .append(lastResponseDate)
                .append(", stats=")
                .append(stats)
                .append("]")
                .toString();
    }
}
