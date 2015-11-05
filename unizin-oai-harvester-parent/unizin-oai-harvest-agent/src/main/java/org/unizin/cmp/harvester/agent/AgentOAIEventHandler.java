package org.unizin.cmp.harvester.agent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPOutputStream;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.unizin.cmp.oai.harvester.exception.HarvesterException;
import org.unizin.cmp.oai.harvester.response.RecordOAIEventHandler;

public final class AgentOAIEventHandler
extends RecordOAIEventHandler<HarvestedOAIRecord> {
    private final String baseURL;
    private final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue;
    private final Timeout offerTimeout;
    private final XMLOutputFactory outputFactory;
    private final MessageDigest messageDigest;

    public AgentOAIEventHandler(final URI baseURI,
            final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue,
            final Timeout offerTimeout,
            final XMLOutputFactory outputFactory,
            final MessageDigest messageDigest) {
        this.baseURL = baseURI.toString();
        this.harvestedRecordQueue = harvestedRecordQueue;
        this.offerTimeout = offerTimeout;
        this.outputFactory = outputFactory;
        this.messageDigest = messageDigest;
    }

    private byte[] checksum(final byte[] bytes) {
        messageDigest.update(bytes);
        return messageDigest.digest();
    }

    private void offerRecord(final HarvestedOAIRecord record) {
        try {
            harvestedRecordQueue.offer(record, offerTimeout.getTime(),
                    offerTimeout.getUnit());
        } catch (final InterruptedException e) {
            Thread.interrupted();
            throw new HarvesterException(e);
        }
    }

    private byte[] createXML(final List<XMLEvent> events) throws XMLStreamException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final XMLEventWriter writer = outputFactory.createXMLEventWriter(baos);
        for (final XMLEvent event : events) {
            writer.add(event);
        }
        return baos.toByteArray();
    }

    private byte[] compress(final byte[] rawBytes) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final GZIPOutputStream out = new GZIPOutputStream(baos)) {
            out.write(rawBytes);
        } catch (final IOException e) {
            throw new HarvesterException(e);
        }
        return baos.toByteArray();
    }

    @Override
    protected void onDatestamp(final HarvestedOAIRecord currentRecord,
            final String datestamp) {
        currentRecord.setDatestamp(datestamp);
    }

    @Override
    protected void onIdentifier(final HarvestedOAIRecord currentRecord,
            final String identifier) {
        currentRecord.setIdentifier(identifier);
    }

    @Override
    protected void onSet(final HarvestedOAIRecord currentRecord,
            final String set) {
        currentRecord.getSets().add(set);
    }

    @Override
    protected void onStatus(final HarvestedOAIRecord currentRecord,
            final String status) {
        currentRecord.setStatus(status);
    }

    @Override
    protected void onRecordEnd(final HarvestedOAIRecord currentRecord,
            final List<XMLEvent> recordEvents) {
        try {
            final byte[] bytes = createXML(recordEvents);
            final byte[] checksum = checksum(bytes);
            currentRecord.setXml(compress(bytes));
            currentRecord.setChecksum(checksum);
            offerRecord(currentRecord);
        } catch (final XMLStreamException e) {
            throw new HarvesterException(e);
        }
    }

    @Override
    protected HarvestedOAIRecord createRecord(final StartElement recordStartElement) {
        final HarvestedOAIRecord record = new HarvestedOAIRecord();
        record.setBaseURL(baseURL);
        return record;
    }
}
