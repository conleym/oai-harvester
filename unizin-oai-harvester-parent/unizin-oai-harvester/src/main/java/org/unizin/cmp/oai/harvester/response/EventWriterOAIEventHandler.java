package org.unizin.cmp.oai.harvester.response;

import java.util.Objects;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Event handler that writes events to a wrapped {@link XMLEventWriter}.
 *
 */
public final class EventWriterOAIEventHandler implements OAIEventHandler {

    private final XMLEventWriter eventWriter;

    public EventWriterOAIEventHandler(final XMLEventWriter eventWriter) {
        Objects.requireNonNull(eventWriter, "eventWriter");
        this.eventWriter = eventWriter;
    }

    @Override
    public void onEvent(final XMLEvent e) throws XMLStreamException {
        eventWriter.add(e);
    }

    @Override
    public void close() throws XMLStreamException {
        eventWriter.close();
    }
}
