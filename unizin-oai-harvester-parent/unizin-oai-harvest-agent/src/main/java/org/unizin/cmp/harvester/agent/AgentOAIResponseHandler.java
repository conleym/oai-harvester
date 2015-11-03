package org.unizin.cmp.harvester.agent;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLOutputFactory;

import org.unizin.cmp.oai.OAIXMLUtils;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.response.AbstractOAIResponseHandler;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;


public final class AgentOAIResponseHandler extends AbstractOAIResponseHandler {
    private static final String DIGEST_ALGORITHM = "MD5";
    private static final long DEFAULT_TIMEOUT = 100;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;


    private final AgentOAIEventHandler handler;


    public AgentOAIResponseHandler(final URI baseURI,
            final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue)
                    throws NoSuchAlgorithmException{
        this(baseURI, harvestedRecordQueue, DEFAULT_TIMEOUT,
                DEFAULT_TIME_UNIT);
    }

    public AgentOAIResponseHandler(final URI baseURI,
            final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue,
            final long offerTimeout, final TimeUnit offerTimeoutUnit)
                    throws NoSuchAlgorithmException {
        this(baseURI, harvestedRecordQueue, offerTimeout, offerTimeoutUnit,
                OAIXMLUtils.newOutputFactory(), digest());
    }


    public AgentOAIResponseHandler(final URI baseURI,
            final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue,
            final long offerTimeout,
            final TimeUnit offerTimeoutUnit,
            final XMLOutputFactory outputFactory,
            final MessageDigest messageDigest) {
        handler = new AgentOAIEventHandler(baseURI, harvestedRecordQueue,
                offerTimeout, offerTimeoutUnit, outputFactory, messageDigest);
    }


    public static final MessageDigest digest()
            throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(DIGEST_ALGORITHM);
    }


    @Override
    public OAIEventHandler getEventHandler(final HarvestNotification notification) {
        return handler;
    }
}
