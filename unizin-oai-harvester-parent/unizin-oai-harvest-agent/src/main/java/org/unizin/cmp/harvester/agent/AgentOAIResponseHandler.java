package org.unizin.cmp.harvester.agent;

import java.net.URI;
import java.security.MessageDigest;
import java.util.concurrent.BlockingQueue;

import javax.xml.stream.XMLOutputFactory;

import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;

/**
 *
 */
public final class AgentOAIResponseHandler implements OAIResponseHandler {

    private final AgentOAIEventHandler handler;

    public AgentOAIResponseHandler(final URI baseURI,
            final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue,
            final XMLOutputFactory outputFactory,
            final MessageDigest messageDigest) {
        handler = new AgentOAIEventHandler(baseURI, harvestedRecordQueue,
                outputFactory, messageDigest);
    }

    @Override
    public OAIEventHandler getEventHandler(final HarvestNotification notification) {
        return handler;
    }

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
