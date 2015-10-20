package org.unizin.cmp.oai.harvester.response;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * {@link OAIEventHandler} with optional event filters. Events accepted by all
 * filters will be written to the supplied event writer.
 */
public final class FilteringOAIEventHandler implements OAIEventHandler {
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

	@Override
	public void onEvent(final XMLEvent event) throws XMLStreamException {
		for (final EventFilter filter : filters) {
			if (! filter.accept(event)) {
				return;
			}
		}
		eventWriter.add(event);
	}
}
