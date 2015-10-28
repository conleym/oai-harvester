package org.unizin.cmp.oai.harvester.response;

import org.unizin.cmp.oai.harvester.HarvestNotification;

/**
 * Implementations monitor harvests and produce {@link OAIEventHandler}
 * instances to consume {@link javax.xml.stream.events.XMLEvent XMLEvents} from
 * OAI-PMH responses.
 * <p>
 * Instances may have internal state that is affected by calls to the various
 * methods. For example, an implementation could return a different event
 * handler for each response by creating a new instance in the
 * {@link #onResponseReceived(HarvestNotification)} method and returning that
 * instance from {@link #getEventHandler(HarvestNotification)}.
 *
 */
public interface OAIResponseHandler {
    /**
     * Get the {@link OAIEventHandler} to which this response's events should be
     * sent.
     * <p>
     * Called with a
     * {@link org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType#RESPONSE_RECEIVED}
     * notification just after the response is received from the server.
     *
     * @param notification
     *            the current state of the harvest.
     * @return the event handler to use for this response.
     */
    OAIEventHandler getEventHandler(HarvestNotification notification);

    /**
     * Called whenever a harvest starts.
     * <p>
     * Called with a
     * {@link org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType#HARVEST_STARTED}
     * notification just after the harvest starts.
     *
     * @param notification
     *            the current state of the harvest.
     */
    void onHarvestStart(HarvestNotification notification);

    /**
     * Called whenever a harvest ends.
     * <p>
     * Always called, whether the harvest ended normally or was terminated by an
     * error of some kind with a
     * {@link org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType#HARVEST_ENDED}
     * notification.
     *
     * @param notification
     *            the current state of the harvest.
     */
    void onHarvestEnd(HarvestNotification notification);

    /**
     * Called whenever the harvester has received a response from the server,
     * but before it has been processed.
     * <p>
     * Always called, regardless of the nature of the HTTP response from the
     * server, with a
     * {@link org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType#RESPONSE_RECEIVED}
     * notification.
     * <p>
     * For list requests, this will be called just before each incomplete list
     * received from the server is processed.
     *
     * @param notification
     *            the current state of the harvest.
     */
    void onResponseReceived(HarvestNotification notification);

    /**
     * Called when the harvester has finished processing a response from the
     * server.
     * <p>
     * Called whether processing succeeded or failed with a
     * {@link org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType#RESPONSE_PROCESSED}
     * notification. Check the notification object for more information.
     * <p>
     * For list requests, this will be called just after each incomplete list
     * received from the server is processed.
     *
     * @param notification
     *            the current state of the harvest.
     */
    void onResponseProcessed(HarvestNotification notification);
}
