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
 * {@link OAIEventHandler} with optional event filters. Events accepted by all
 * filters will be written to the supplied event writer.
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
		for (final EventFilter filter : filters) {
			if (! filter.accept(event)) {
				return false;
			}
		}
		return true;
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
