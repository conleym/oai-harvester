package org.unizin.cmp.oai.harvester.response;

import static org.unizin.cmp.oai.OAI2Constants.OAI_PMH;
import static org.unizin.cmp.oai.OAI2Constants.REQUEST;
import static org.unizin.cmp.oai.OAI2Constants.RESPONSE_DATE;
import static org.unizin.cmp.oai.OAI2Constants.RESUMPTION_TOKEN;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.OAIXMLUtils;
import org.unizin.cmp.oai.harvester.Functions;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.exception.OAIProtocolException;

/**
 * Merges a series of <a href=
 * "http://www.openarchives.org/OAI/openarchivesprotocol.html#FlowControl">
 * incomplete lists</a> into a single complete list.
 *
 */
public final class MergingOAIResponseHandler
extends AbstractOAIResponseHandler {

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
    private final OAIEventHandler delegate;
    private final XMLEventFactory eventFactory;


    public MergingOAIResponseHandler(final OAIEventHandler delegate) {
        this(delegate, OAIXMLUtils.newEventFactory());
    }

    public MergingOAIResponseHandler(final OAIEventHandler delegate,
            final XMLEventFactory eventFactory) {
        this.eventHandler = new FilteringOAIEventHandler(delegate,
                mergingFilters());
        this.delegate = delegate;
        this.eventFactory = eventFactory;
    }

    @Override
    public OAIEventHandler getEventHandler(
            final HarvestNotification notification) {
        return eventHandler;
    }

    /**
     * Should we write the epilogue to the event handler?
     * <p>
     * The filter doesn't know on its own when the harvest ends, so it will have
     * filtered out the final closing events. We have to add them ourselves to
     * make valid XML.
     * </p>
     * <p>
     * We only do this when we know the output is in a reasonable state, i.e.,
     * that adding these events will not result in XMLStreamExceptions. We know
     * this when a harvest has been successful (i.e., has no errors), has a
     * protocol error only, or was stopped (<em>not</em> cancelled). Otherwise,
     * we avoid adding events that will likely compound other errors or add
     * unwanted noise to the output of a cancelled harvest.
     * </p>
     *
     * @param notification
     *            the harvest end notification.
     * @return {@code true} iff we should write the epilogue.
     */
    private boolean shouldAddEpilogue(final HarvestNotification notification) {
        final Exception ex = notification.getException();
        final boolean exOK = (ex == null ||
                ex instanceof OAIProtocolException);
        return exOK && !notification.isCancelled();
    }

    /**
     * Add an epilogue directly to the delegate (our own event handler would
     * just filter it out).
     *
     * @param notification
     *            the harvest end notification.
     * @throws XMLStreamException
     *             if there's an error adding the epilogue.
     */
    private void addEpilogue(final HarvestNotification notification)
    throws XMLStreamException {
        if (shouldAddEpilogue(notification)) {
            // Not needed with woodstox, but required with the JDK/Xerces.
            delegate.onEvent(eventFactory.createEndElement(
                    notification.getVerb().qname(), null));
            delegate.onEvent(eventFactory.createEndElement(OAI_PMH, null));
            delegate.onEvent(eventFactory.createEndDocument());
        }
    }

    private void close() throws XMLStreamException {
        eventHandler.close();
    }

    @Override
    public void onHarvestEnd(final HarvestNotification notification) {
        final Runnable tryCall = Functions.wrap(this::addEpilogue,
                notification);
        final Runnable finallyCall = Functions.wrap(this::close);
        Functions.suppressExceptions(tryCall, finallyCall);
    }
}
