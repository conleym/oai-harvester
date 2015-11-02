package org.unizin.cmp.oai.harvester.response;

import static org.unizin.cmp.oai.OAI2Constants.OAI_PMH;
import static org.unizin.cmp.oai.OAI2Constants.REQUEST;
import static org.unizin.cmp.oai.OAI2Constants.RESPONSE_DATE;
import static org.unizin.cmp.oai.OAI2Constants.RESUMPTION_TOKEN;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.OAIXMLUtils;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;

/**
 * Merges a series of <a href=
 * "http://www.openarchives.org/OAI/openarchivesprotocol.html#FlowControl">
 * incomplete lists</a> into a single complete list.
 *
 */
public final class MergingOAIResponseHandler extends AbstractOAIResponseHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MergingOAIResponseHandler.class);

    /**
    * Filter that skips certain elements in order to make a series of
    * incomplete list responses appear as a single complete list.
    *
    * All but the first of the following are skipped:
    * <ul>
    * <li>start document</li>
    * <li>OAI-PMH</li>
    * <li>request</li>
    * <li>responseDate</li>
    * <li>any verb's start element</li>
    * </ul>
    *
    * All of the following are skipped:
    * <ul>
    * <li>resumptionToken</li>
    * <li>end document</li>
    *
    */
    private static final class MergingEventFilter implements EventFilter {
        private boolean skipping;
        private boolean startDoc;
        private boolean oaipmh;
        private boolean responseDate;
        private boolean request;
        private boolean verb;

        @Override
        public boolean accept(final XMLEvent event) {
            if (event.isStartElement()) {
                final StartElement se = event.asStartElement();
                final QName name = se.getName();
                if (OAI_PMH.equals(name)) {
                    skipping = oaipmh;
                    oaipmh = true;
                } else if (REQUEST.equals(name)) {
                    skipping = request;
                } else if (OAIVerb.isVerb(name)) {
                    skipping = verb;
                    verb = true;
                } else if (RESUMPTION_TOKEN.equals(name)) {
                    skipping = true;
                } else if (RESPONSE_DATE.equals(name)) {
                    skipping = responseDate;
                } else {
                    skipping = false;
                }
            } else if (event.isEndElement()) {
                // Start skipping some elements only after accepting the first
                // start element PLUS its contents and matching end element.
                final EndElement ee = event.asEndElement();
                final QName name =ee.getName();
                if (RESPONSE_DATE.equals(name)) {
                    skipping = responseDate;
                    responseDate = true;
                } else if (REQUEST.equals(name)) {
                    skipping = request;
                    request = true;
                } else if (OAIVerb.isVerb(name)) {
                    skipping = true;
                } else if (OAI_PMH.equals(name)) {
                    skipping = true;
                }
            } else if (event.isStartDocument()) {
                skipping = startDoc || skipping;
                startDoc = true;
            } else if (event.isEndDocument()) {
                skipping = true;
            }
            return !skipping;
        }
    }

    public static final List<EventFilter> mergingFilters() {
        return Arrays.asList(new MergingEventFilter());
    }


    private final OAIEventHandler eventHandler;
    private final XMLEventWriter eventWriter;
    private final XMLEventFactory eventFactory;

    public MergingOAIResponseHandler(final OutputStream out)
            throws XMLStreamException {
        this(out, OAIXMLUtils.newOutputFactory(),
                OAIXMLUtils.newEventFactory());
    }

    public MergingOAIResponseHandler(final OutputStream out,
            final XMLOutputFactory outputFactory,
            final XMLEventFactory eventFactory) throws XMLStreamException {
        this.eventWriter = outputFactory.createXMLEventWriter(out);
        this.eventHandler = new FilteringOAIEventHandler(eventWriter,
                mergingFilters());
        this.eventFactory = eventFactory;
    }

    @Override
    public OAIEventHandler getEventHandler(final HarvestNotification notification) {
        return eventHandler;
    }

    @Override
    public void onHarvestEnd(final HarvestNotification notification) {
        LOGGER.debug("Harvest has ended.");
        try {
            if (! notification.hasError()) {
                /*
                * The filter doesn't know on its own when the harvest ends, so
                * it will have filtered out the final closing events. We have
                * to add them ourselves to make valid XML.
                *
                * We only do this when the harvest has been successful, as we
                * cannot know what events have been written, and thus cannot
                * reliably write events.
                *
                */
                eventWriter.add(eventFactory.createEndElement(
                        notification.getVerb().qname(), null));
                eventWriter.add(eventFactory.createEndElement(OAI_PMH, null));
                eventWriter.add(eventFactory.createEndDocument());
            }
        } catch (final XMLStreamException e) {
            throw new HarvesterException(e);
        } finally {
            OAIXMLUtils.closeQuietly(eventWriter);
        }
    }

    @Override
    public void onResponseProcessed(final HarvestNotification notification) {
        try {
            eventWriter.flush();
        } catch (final XMLStreamException e) {
            throw new HarvesterException(e);
        }
    }
}
