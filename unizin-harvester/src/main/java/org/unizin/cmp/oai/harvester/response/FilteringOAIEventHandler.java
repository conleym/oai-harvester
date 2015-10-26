package org.unizin.cmp.oai.harvester.response;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link OAIEventHandler} with and optional list of event filters. Events
 * accepted by all filters will be written to the supplied event writer.
 * <p>
 * The filters are applied in order, and each event is sent to every filter in
 * the list.
 */
public final class FilteringOAIEventHandler implements OAIEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            FilteringOAIEventHandler.class);

    private final XMLEventWriter eventWriter;
    private final List<EventFilter> filters;

    public FilteringOAIEventHandler(final XMLEventWriter eventWriter) {
        this(eventWriter, Collections.emptyList());
    }

    public FilteringOAIEventHandler(final XMLEventWriter eventWriter,
            final List<EventFilter> filters) {
        this.eventWriter = eventWriter;
        this.filters = filters;
    }

    private boolean accept(final XMLEvent event) {
        boolean result = true;
        for (final EventFilter filter : filters) {
            if (! filter.accept(event)) {
                /*
                 * No return here!
                 *
                 * We deliberately send the event to ALL the filters, even if it
                 * has been rejected, because filters might have internal state
                 * and depend upon seeing all elements for correct operation.
                 */
                result = false;
            }
        }
        return result;
    }

    @Override
    public void onEvent(final XMLEvent event) throws XMLStreamException {
        final boolean accept = accept(event);
        if (accept) {
            LOGGER.trace("Accepting event {}", event);
            eventWriter.add(event);
        } else {
            LOGGER.trace("Rejecting event {}", event);
        }
    }
}
