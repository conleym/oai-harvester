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
    private static final String DIGEST_ALGORITHM = "MD5";

    public static final MessageDigest digest()
            throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(DIGEST_ALGORITHM);
    }


    private final AgentOAIEventHandler handler;

    public AgentOAIResponseHandler(final URI baseURI,
            final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue)
                    throws NoSuchAlgorithmException {
        this(baseURI, harvestedRecordQueue, OAIXMLUtils.newOutputFactory(),
                digest());
    }

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
