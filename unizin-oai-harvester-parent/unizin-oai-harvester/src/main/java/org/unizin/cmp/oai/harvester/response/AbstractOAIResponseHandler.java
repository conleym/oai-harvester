package org.unizin.cmp.oai.harvester.response;

import org.unizin.cmp.oai.harvester.HarvestNotification;


/**
 * {@link OAIResponseHandler} that does nothing when notified of harvest events.
 * <p>
 * Subclasses must implement
 * {@link OAIResponseHandler#getEventHandler(HarvestNotification)}.
 * <p>
 * This class should be considered an implementation detail of its subclasses.
 * There should be no {@code instanceof AbstractOAIResponseHandler} checks in
 * any code, it should not be used as the type of method parameters, etc.
 *
 */
public abstract class AbstractOAIResponseHandler implements OAIResponseHandler {
    @Override
    public void onHarvestStart(final HarvestNotification notification) {
    }

    @Override
    public void onHarvestEnd(final HarvestNotification notification) {
    }

    @Override
    public void onResponseReceived(final HarvestNotification notification) {
    }

    @Override
    public void onResponseProcessed(final HarvestNotification notification) {
    }
}
