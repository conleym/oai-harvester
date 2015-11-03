package org.unizin.cmp.harvester.agent;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;

import javax.xml.stream.XMLOutputFactory;

import org.unizin.cmp.oai.OAIXMLUtils;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.response.AbstractOAIResponseHandler;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;


public final class AgentOAIResponseHandler extends AbstractOAIResponseHandler {
    public static final String DIGEST_ALGORITHM = "MD5";

    private final AgentOAIEventHandler handler;


    public AgentOAIResponseHandler(final URI baseURI,
            final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue)
                    throws NoSuchAlgorithmException{
        this(baseURI, harvestedRecordQueue, HarvestAgent.DEFAULT_TIMEOUT);
    }

    public AgentOAIResponseHandler(final URI baseURI,
            final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue,
            final Timeout offerTimeout)
                    throws NoSuchAlgorithmException {
        this(baseURI, harvestedRecordQueue, offerTimeout,
                OAIXMLUtils.newOutputFactory(), HarvestAgent.digest());
    }


    public AgentOAIResponseHandler(final URI baseURI,
            final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue,
            final Timeout offerTimeout,
            final XMLOutputFactory outputFactory,
            final MessageDigest messageDigest) {
        handler = new AgentOAIEventHandler(baseURI, harvestedRecordQueue,
                offerTimeout, outputFactory, messageDigest);
    }


    @Override
    public OAIEventHandler getEventHandler(final HarvestNotification notification) {
        return handler;
    }
}
