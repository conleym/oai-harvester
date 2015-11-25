package org.unizin.cmp.oai.mocks;

import static org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType.HARVEST_ENDED;
import static org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType.HARVEST_STARTED;
import static org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType.RESPONSE_PROCESSED;
import static org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType.RESPONSE_RECEIVED;

import java.util.Objects;
import java.util.function.Predicate;

import org.unizin.cmp.oai.ResumptionToken;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestStatistic;

public final class NotificationMatchers {
    public static final Predicate<HarvestNotification> RUNNING = (hn) -> {
        return hn.isRunning() && !hn.isExplicitlyStopped() && !hn.hasError();
    };

    public static final Predicate<HarvestNotification> NOT_RUNNING = (hn) -> {
        return !hn.isRunning() && !hn.isExplicitlyStopped() && !hn.hasError();
    };

    public static final Predicate<HarvestNotification> HAS_ERROR = (hn) -> {
        return !hn.isRunning() && !hn.isExplicitlyStopped() && hn.hasError();
    };

    public static final HarvestNotification harvestStarted() {
        return Mocks.matcherFromPredicate(
                (hn) -> {
                    return hn.getType() == HARVEST_STARTED &&
                            NotificationMatchers.RUNNING.test(hn);
                },
                HarvestNotification.class);
    }

    public static final HarvestNotification responseReceived() {
        return Mocks.matcherFromPredicate(
                (hn) -> {
                    return hn.getType() == RESPONSE_RECEIVED &&
                            NotificationMatchers.RUNNING.test(hn);
                },
                HarvestNotification.class);
    }

    public static final HarvestNotification responseProcessedWithError() {
        return Mocks.matcherFromPredicate(
                (hn) -> {
                    return hn.getType() == RESPONSE_PROCESSED &&
                            NotificationMatchers.HAS_ERROR.test(hn);
                },
                HarvestNotification.class);
    }

    public static final HarvestNotification responseProcessedSuccessfully() {
        return Mocks.matcherFromPredicate(
                (hn) -> {
                    return hn.getType() == RESPONSE_PROCESSED &&
                            NotificationMatchers.RUNNING.test(hn);
                },
                HarvestNotification.class);
    }

    public static final HarvestNotification lastResponseProcessedSuccessfully() {
        return Mocks.matcherFromPredicate((hn) -> {
            return hn.getType() == RESPONSE_PROCESSED &&
                    !hn.isRunning() && !hn.isExplicitlyStopped() && !hn.hasError();
        },
                HarvestNotification.class);
    }

    public static final HarvestNotification harvestEndedWithError() {
        return Mocks.matcherFromPredicate((hn) -> {
            return hn.getType() == HARVEST_ENDED &&
                    NotificationMatchers.HAS_ERROR.test(hn);
        },
                HarvestNotification.class);
    }

    public static final HarvestNotification harvestEndedSuccessfully() {
        return Mocks.matcherFromPredicate((hn) -> {
            return hn.getType() == HARVEST_ENDED &&
                    NotificationMatchers.NOT_RUNNING.test(hn);
        },
                HarvestNotification.class);
    }

    public static final HarvestNotification withStats(final long requestCount,
            final long responseCount) {
        return Mocks.matcherFromPredicate((hn) -> {
            return hn.getStat(HarvestStatistic.REQUEST_COUNT) == requestCount &&
                    hn.getStat(HarvestStatistic.RESPONSE_COUNT) == responseCount;
        },
                HarvestNotification.class);
    }

    public static final HarvestNotification withToken(final ResumptionToken token) {
        return Mocks.matcherFromPredicate((hn) -> {
            return Objects.equals(token, hn.getResumptionToken());
        },
                HarvestNotification.class);
    }

    /** No instances allowed. */
    private NotificationMatchers() {}
}
