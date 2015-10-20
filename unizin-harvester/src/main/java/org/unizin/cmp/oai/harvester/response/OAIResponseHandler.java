package org.unizin.cmp.oai.harvester.response;

import org.unizin.cmp.oai.harvester.HarvestNotification;

/**
 * Implementations decide how to handle OAI-PMH responses.
 */
public interface OAIResponseHandler {
	OAIEventHandler getEventHandler(HarvestNotification notification);
	void onHarvestStart(HarvestNotification notification);
	void onHarvestEnd(HarvestNotification notification);
	void onResponseStart(HarvestNotification notification);
	void onResponseEnd(HarvestNotification notification);
}
