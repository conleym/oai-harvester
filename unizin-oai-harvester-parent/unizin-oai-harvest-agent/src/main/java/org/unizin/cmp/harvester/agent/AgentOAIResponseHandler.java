package org.unizin.cmp.harvester.agent;

import java.net.URI;
import java.security.MessageDigest;
import java.util.concurrent.BlockingQueue;

import javax.xml.stream.XMLOutputFactory;

import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.response.AbstractOAIResponseHandler;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;

/**
 *
 */
public final class AgentOAIResponseHandler extends AbstractOAIResponseHandler {

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
}
