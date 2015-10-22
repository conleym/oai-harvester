package org.unizin.cmp.oai.mocks;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;

public final class NullOAIResponseHandler implements OAIResponseHandler {
	
	public static final class NullOAIEventHandler implements OAIEventHandler {
		@Override
		public void onEvent(XMLEvent e) throws XMLStreamException {			
		}
	}

	private final OAIEventHandler handler = new NullOAIEventHandler();
	
	@Override
	public OAIEventHandler getEventHandler(HarvestNotification notification) {
		return handler;
	}

	@Override
	public void onHarvestStart(HarvestNotification notification) {
	}

	@Override
	public void onHarvestEnd(HarvestNotification notification) {
	}

	@Override
	public void onResponseReceived(HarvestNotification notification) {
	}

	@Override
	public void onResponseProcessed(HarvestNotification notification) {
	}
}
