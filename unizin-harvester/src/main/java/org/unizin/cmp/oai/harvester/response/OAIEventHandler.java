package org.unizin.cmp.oai.harvester.response;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Instances receive all {@link XMLEvent XMLEvents} from each response and
 * handle them appropriately.
 * <p>
 * Instances are created or dispensed per-response by a corresponding
 * {@link OAIResponseHandler}.
 * <p>
 * Instances are free to write events to an
 * {@link javax.xml.stream.XMLEventWriter} or not. Filtering is best
 * accomplished by adding {@link javax.xml.stream.EventFilter EventFilters} to a
 * custom event handler, as filters are easily composed.
 *
 */
public interface OAIEventHandler {
	void onEvent(XMLEvent e) throws XMLStreamException;
}
