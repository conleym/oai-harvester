package org.unizin.cmp.oai.harvester.job;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import javax.xml.stream.XMLOutputFactory;

import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;
import org.unizin.cmp.oai.harvester.response.AbstractOAIResponseHandler;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;


/**
 * Response handler that receives {@link HarvestedOAIRecord} instances and
 * offers them to a {@link BlockingQueue} for consumption by another thread.
 *
 */
public final class JobOAIResponseHandler extends AbstractOAIResponseHandler
implements Consumer<HarvestedOAIRecord> {
    private final JobOAIEventHandler handler;
    private final BlockingQueueWrapper<HarvestedOAIRecord> harvestedRecordQueue;


    public JobOAIResponseHandler(final URI baseURI,
            final BlockingQueueWrapper<HarvestedOAIRecord> harvestedRecordQueue)
                    throws NoSuchAlgorithmException {
        this(baseURI, harvestedRecordQueue,
                JobOAIEventHandler.defaultOutputFactory(), HarvestJob.digest());
    }

    public JobOAIResponseHandler(final URI baseURI,
            final BlockingQueueWrapper<HarvestedOAIRecord> harvestedRecordQueue,
            final XMLOutputFactory outputFactory,
            final MessageDigest messageDigest) {
        handler = new JobOAIEventHandler(baseURI, this, outputFactory,
                messageDigest);
        this.harvestedRecordQueue = harvestedRecordQueue;
    }

    @Override
    public OAIEventHandler getEventHandler(
            final HarvestNotification notification) {
        return handler;
    }

    @Override
    public void accept(final HarvestedOAIRecord record) {
        try {
            if (!harvestedRecordQueue.offer(record)) {
                throw new HarvesterException(String.format(
                        "Timed out after %s trying to offer record.",
                        harvestedRecordQueue.getOfferTimeout()));
            }
        } catch (final InterruptedException e) {
            // Interrupting the thread ensures that the harvest ends
            // after the current response is processed.
            Thread.currentThread().interrupt();
            // This will stop the harvest _now_. We'll preserve the interrupted
            // status anyway.
            throw new HarvesterException(e);
        }
    }
}
