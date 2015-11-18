package org.unizin.cmp.oai.harvester.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.OAI2Constants;
import org.unizin.cmp.oai.OAIXMLUtils;


/**
 * Event handler implementation that produces record objects.
 * <p>
 * Instances have a {@link Consumer} to which finalized records are sent once
 * finalized for further processing.
 * </p>
 *
 * <h2>Cancelled Harvests</h2>
 * <p>
 * Because each record is buffered until complete, this handler can guarantee
 * that each processed record is valid and complete, even if the harvest is
 * cancelled.
 * </p>
 *
 * @param <T>
 *            the type of the record object.
 */
public abstract class RecordOAIEventHandler<T>
implements OAIEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            RecordOAIEventHandler.class);

    private List<XMLEvent> eventBuffer = new ArrayList<>();
    private final StringBuilder charBuffer = new StringBuilder();
    private final Consumer<T> recordHandler;
    private T currentRecord;
    private boolean inRecord;
    private boolean bufferChars;
    private QName currentStartElementQName;

    protected RecordOAIEventHandler(final Consumer<T> recordHandler) {
        this.recordHandler = recordHandler;
    }

    private boolean currentElementIs(final QName name) {
        return Objects.equals(currentStartElementQName, name);
    }

    private String getBufferedChars() {
        return charBuffer.toString();
    }

    private List<XMLEvent> copyAndClearBuffer() {
        final List<XMLEvent> copy = new ArrayList<>(eventBuffer);
        eventBuffer.clear();
        return copy;
    }

    @Override
    public void onEvent(final XMLEvent e) throws XMLStreamException {
        LOGGER.trace("Got event {}", e);
        if (e.isStartElement()) {
            currentStartElementQName = e.asStartElement().getName();
            if (currentElementIs(OAI2Constants.RECORD)) {
                inRecord = true;
                currentRecord = createRecord(e.asStartElement());
            } else if (currentElementIs(OAI2Constants.HEADER)) {
                final String status = OAIXMLUtils.attributeValue(
                        e.asStartElement(), OAI2Constants.HEADER_STATUS_ATTR);
                onStatus(currentRecord, status);
            } else if (currentElementIs(OAI2Constants.DATESTAMP) ||
                    currentElementIs(OAI2Constants.IDENTIFIER) ||
                    currentElementIs(OAI2Constants.SET_SPEC)) {
                bufferChars = true;
            }
        } else if (e.isEndElement()) {
            final QName name = e.asEndElement().getName();
            if (OAI2Constants.RECORD.equals(name)) {
                inRecord = false;
                eventBuffer.add(e);
                onRecordEnd(currentRecord, copyAndClearBuffer());
                recordHandler.accept(currentRecord);
            } else if (OAI2Constants.IDENTIFIER.equals(name)) {
                final String identifier = getBufferedChars();
                LOGGER.trace("Setting identifier {}", identifier);
                onIdentifier(currentRecord, identifier);
            } else if (OAI2Constants.DATESTAMP.equals(name)) {
                final String datestamp = getBufferedChars();
                LOGGER.trace("Setting datestamp {}", datestamp);
                onDatestamp(currentRecord, datestamp);
            } else if (OAI2Constants.SET_SPEC.equals(name)) {
                final String set = getBufferedChars();
                LOGGER.trace("Adding set {}", set);
                onSet(currentRecord, set);
            }
            // We never want to buffer characters beyond an end tag.
            bufferChars = false;
            charBuffer.setLength(0);
        } else if (e.isCharacters() && bufferChars) {
            charBuffer.append(e.asCharacters().getData());
        } else if (currentElementIs(OAI2Constants.HEADER) && e.isAttribute()) {
            // Not sure this ever happens....
            final Attribute a = (Attribute)e;
            if (OAI2Constants.HEADER_STATUS_ATTR.equals(a.getName())) {
                final String status = a.getValue();
                onStatus(currentRecord, status);
            }
        }
        if (inRecord) {
            eventBuffer.add(e);
        }
    }

    protected abstract void onDatestamp(T currentRecord, String datestamp);
    protected abstract void onIdentifier(T currentRecord, String identifier);
    protected abstract void onSet(T currentRecord, String set);
    protected abstract void onStatus(T currentRecord, String status);

    /**
     * Called when &lt;/record&gt; is seen to finalize the current record
     * object.
     * <p>
     * This method is called with the current record and a list of XMLEvents
     * accumulated since the beginning of the record (and including the end
     * element). This list is mutable, and subclass implementors should consider
     * themselves free to modify it as needed.
     * </p>
     *
     * @param currentRecord
     *            the current record object.
     * @param recordEvents
     *            a list containing all events since the last &lt;record&gt;,
     *            including the matching end element.
     */
    protected abstract void onRecordEnd(T currentRecord,
            List<XMLEvent> recordEvents);

    /**
     * Implementations must create and return a new instance of the record object.
     *
     * @param recordStartElement the start element of the next record to process.
     * @return a new instance of the record object.
     */
    protected abstract T createRecord(final StartElement recordStartElement);
}
