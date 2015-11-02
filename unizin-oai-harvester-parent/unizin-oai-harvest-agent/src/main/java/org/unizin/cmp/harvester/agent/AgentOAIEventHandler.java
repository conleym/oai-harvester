package org.unizin.cmp.harvester.agent;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.OAI2Constants;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;

public final class AgentOAIEventHandler implements OAIEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            AgentOAIEventHandler.class);

    private final String baseURL;
    private final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue;
    private final XMLOutputFactory outputFactory;
    private final MessageDigest messageDigest;
    private final List<XMLEvent> eventBuffer = new ArrayList<>();
    private final StringBuilder charBuffer = new StringBuilder();

    private boolean inRecord;
    private boolean bufferChars;
    private QName currentStartElementQName;
    private HarvestedOAIRecord currentRecord = new HarvestedOAIRecord();


    public AgentOAIEventHandler(final URI baseURI,
            final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue,
            final XMLOutputFactory outputFactory,
            final MessageDigest messageDigest) {
        this.baseURL = baseURI.toString();
        this.harvestedRecordQueue = harvestedRecordQueue;
        this.outputFactory = outputFactory;
        this.messageDigest = messageDigest;
    }


    private boolean currentElementIs(final QName name) {
        return Objects.equals(currentStartElementQName, name);
    }


    private byte[] checksum(final byte[] bytes) {
        messageDigest.update(bytes);
        return messageDigest.digest();
    }


    private boolean isCurrentRecordValid() {
        return Arrays.asList(currentRecord.getBaseURL(),
                currentRecord.getXml(), currentRecord.getIdentifier(),
                currentRecord.getDatestamp())
                .stream()
                .noneMatch(Predicate.isEqual(null));
    }


    private void offerCurrentRecord() throws XMLStreamException {
        // Set up for next record.
        final HarvestedOAIRecord previousRecord = currentRecord;
        currentRecord = new HarvestedOAIRecord();

        // Finalize the record to add.
        final byte[] recordBytes = getAndClearEventBuffer();
        previousRecord.setXml(new String(recordBytes, StandardCharsets.UTF_8));
        previousRecord.setChecksum(checksum(recordBytes));
        previousRecord.setBaseURL(baseURL);
        if (! isCurrentRecordValid()) {
            // This is more of a sanity check of the code than anything.
            LOGGER.warn("Invalid record! Skipping: {}", previousRecord);
            return;
        }
        try {
            harvestedRecordQueue.offer(previousRecord, 100, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            Thread.interrupted();
            throw new HarvesterException(e);
        }
    }


    private String getAndClearCharBuffer() {
        final String value = charBuffer.toString();
        charBuffer.setLength(0);
        return value;
    }


    private byte[] getAndClearEventBuffer() throws XMLStreamException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            final XMLEventWriter writer = outputFactory.createXMLEventWriter(baos);
            for (final XMLEvent event : eventBuffer) {
                writer.add(event);
            }
            return baos.toByteArray();
        } finally {
            /*
             * Clear the buffer regardless of exceptions so that it's possible
             * to continue with the next record if desired.
             */
            eventBuffer.clear();
        }
    }


    @Override
    public void onEvent(final XMLEvent e) throws XMLStreamException {
        LOGGER.debug("Got event {}", e);
        if (e.isStartElement()) {
            currentStartElementQName = e.asStartElement().getName();
            if (currentElementIs(OAI2Constants.RECORD)) {
                inRecord = true;
            } else if (currentElementIs(OAI2Constants.DATESTAMP) ||
                    currentElementIs(OAI2Constants.IDENTIFIER) ||
                    currentElementIs(OAI2Constants.SET_SPEC)) {
                bufferChars = true;
            }
        } else if (e.isEndElement()) {
            // We never want to buffer characters beyond an end tag.
            bufferChars = false;
            final QName name = e.asEndElement().getName();
            if (OAI2Constants.RECORD.equals(name)) {
                inRecord = false;
                eventBuffer.add(e);
                offerCurrentRecord();
            } else if (OAI2Constants.IDENTIFIER.equals(name)) {
                currentRecord.setIdentifier(getAndClearCharBuffer());
            } else if (OAI2Constants.DATESTAMP.equals(name)) {
                currentRecord.setDatestamp(getAndClearCharBuffer());
            } else if (OAI2Constants.SET_SPEC.equals(name)) {
                currentRecord.getSets().add(getAndClearCharBuffer());
            }
        } else if (e.isCharacters() && bufferChars) {
            charBuffer.append(e.asCharacters().getData());
        } else if (currentElementIs(OAI2Constants.HEADER) && e.isAttribute()) {
            final Attribute a = (Attribute)e;
            if (OAI2Constants.HEADER_STATUS_ATTR.equals(a.getName())) {
                currentRecord.setStatus(a.getValue());
            }
        }
        if (inRecord) {
            eventBuffer.add(e);
        }
    }
}
