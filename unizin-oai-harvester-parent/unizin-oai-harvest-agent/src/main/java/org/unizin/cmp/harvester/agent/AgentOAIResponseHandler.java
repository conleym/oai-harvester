package org.unizin.cmp.harvester.agent;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import javax.xml.stream.XMLOutputFactory;

import org.unizin.cmp.oai.OAIXMLUtils;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;
import org.unizin.cmp.oai.harvester.response.AbstractOAIResponseHandler;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;


/**
 * Response handler that receives {@link HarvestedOAIRecord} instances and
 * offers them to a {@link BlockingQueue} for consumption by another thread.
 *
 */
public final class AgentOAIResponseHandler extends AbstractOAIResponseHandler
implements Consumer<HarvestedOAIRecord> {
    private final AgentOAIEventHandler handler;

    private final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue;
    private final Timeout offerTimeout;


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
        handler = new AgentOAIEventHandler(baseURI, this, outputFactory,
                messageDigest);
        this.harvestedRecordQueue = harvestedRecordQueue;
        this.offerTimeout = offerTimeout;
    }

    @Override
    public OAIEventHandler getEventHandler(
            final HarvestNotification notification) {
        return handler;
    }

    @Override
    public void accept(final HarvestedOAIRecord record) {
        try {
            harvestedRecordQueue.offer(record, offerTimeout.getTime(),
                    offerTimeout.getUnit());
        } catch (final InterruptedException e) {
            Thread.interrupted();
            // This will stop the harvest.
            throw new HarvesterException(e);
        }
    }
}
