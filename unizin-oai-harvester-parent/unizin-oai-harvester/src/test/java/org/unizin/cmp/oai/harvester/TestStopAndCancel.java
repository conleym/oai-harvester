package org.unizin.cmp.oai.harvester;

import static org.mockito.Matchers.eq;
import static org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType.HARVEST_ENDED;
import static org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType.RESPONSE_PROCESSED;
import static org.unizin.cmp.oai.harvester.ListResponses.setupWithDefaultListRecordsResponse;
import static org.unizin.cmp.oai.harvester.Tests.newParams;
import static org.unizin.cmp.oai.mocks.Mocks.inOrderVerify;

import java.util.Observer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestStatistic;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;
import org.unizin.cmp.oai.mocks.Mocks;
import org.unizin.cmp.oai.mocks.NotificationMatchers;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * Tests the {@link Harvester#stop()} and {@link Harvester#cancel()} methods.
 */
public final class TestStopAndCancel {
    @Rule
    public final WireMockRule wireMock = WireMockUtils.newWireMockRule();

    private void observerTest(final Consumer<Harvester> action,
            final Predicate<HarvestNotification> lastNotificationCheck)
                    throws Exception {
        setupWithDefaultListRecordsResponse(true);

        /*
         * Create an observer that executes the given action after the first
         * response is processed.
         */
        final Observer obs = (o, arg) -> {
            final HarvestNotification hn = (HarvestNotification)arg;
            if (hn.getType() == RESPONSE_PROCESSED &&
                    hn.getStat(HarvestStatistic.REQUEST_COUNT) == 1L) {
                action.accept((Harvester)o);
            }
        };

        // Set up mocks, observer and run harvest.
        final OAIResponseHandler rh = Mocks.newResponseHandler();
        final Harvester harvester = new Harvester.Builder().build();
        harvester.addObserver(obs);
        final Observer mockObserver = Mockito.mock(Observer.class);
        harvester.addObserver(mockObserver);
        harvester.start(newParams(OAIVerb.LIST_RECORDS).build(), rh);

        // Verify notifications.
        inOrderVerify(rh).onHarvestStart(
                NotificationMatchers.harvestStarted());
        inOrderVerify(mockObserver).update(eq(harvester),
                NotificationMatchers.harvestStarted());
        inOrderVerify(rh).onResponseReceived(
                NotificationMatchers.responseReceived());
        inOrderVerify(mockObserver).update(eq(harvester),
                NotificationMatchers.responseReceived());
        inOrderVerify(rh).onResponseProcessed(
                NotificationMatchers.responseProcessedSuccessfully());
        inOrderVerify(mockObserver).update(eq(harvester),
                NotificationMatchers.responseProcessedSuccessfully());

        final Supplier<HarvestNotification> lastNotification = () -> {
            return AdditionalMatchers.and(
                    NotificationMatchers.withStats(1, 1),
                    Mocks.matcherFromPredicate(lastNotificationCheck,
                            HarvestNotification.class));
        };
        // Harvest ends. Verify second incomplete list not retrieved.
        inOrderVerify(rh).onHarvestEnd(lastNotification.get());
        inOrderVerify(mockObserver).update(eq(harvester),
                lastNotification.get());
    }

    /**
     * Tests that {@code Observers} can stop the harvest via
     * {@link Harvester#stop()}.
     */
    @Test
    public void testStop() throws Exception {
        final Predicate<HarvestNotification> p = (hn) -> {
            return hn.getType() == HARVEST_ENDED &&
                    !hn.isRunning() &&
                    hn.isExplicitlyStopped() &&
                    !hn.isCancelled() &&
                    !hn.isInterrupted() &&
                    !hn.hasError();
        };
        observerTest((h) -> h.stop(), p);
    }

    /**
     * Tests that {@code Observers} can cancel the harvest via
     * {@link Harvester#cancel()}.
     */
    @Test
    public void testCancel() throws Exception {
        final Predicate<HarvestNotification> p = (hn) -> {
            return hn.getType() == HARVEST_ENDED &&
                    !hn.isRunning() &&
                    hn.isExplicitlyStopped() &&
                    hn.isCancelled() &&
                    !hn.isInterrupted() &&
                    !hn.hasError();
        };
        observerTest((h) -> h.cancel(), p);
    }

    /**
     * Tests that interrupting the harvester thread stops the harvest.
     */
    @Test
    public void testInterrupt() throws Exception {
        final Predicate<HarvestNotification> p = (hn) -> {
            return hn.getType() == HARVEST_ENDED &&
                    !hn.isRunning() &&
                    !hn.isExplicitlyStopped() &&
                    !hn.isCancelled() &&
                    hn.isInterrupted() &&
                    !hn.hasError();
        };
        observerTest((h) -> Thread.currentThread().interrupt(), p);
        /*
         * Thread should still have interrupted status, which will be cleared
         * by this check.
         */
        Assert.assertTrue(Thread.interrupted());
    }
}
