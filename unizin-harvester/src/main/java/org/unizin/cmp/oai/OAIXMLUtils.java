package org.unizin.cmp.oai;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class OAIXMLUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            OAIXMLUtils.class);


    public static XMLEventFactory newEventFactory() {
        return XMLEventFactory.newFactory();
    }

    public static XMLInputFactory newInputFactory() {
        final XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.IS_COALESCING, true);
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        return factory;
    }

    public static XMLOutputFactory newOutputFactory() {
        return XMLOutputFactory.newFactory();
    }

    public static String attributeValue(final StartElement se,
            final QName name) {
        final Attribute attr = se.getAttributeByName(name);
        return (attr == null) ? null : attr.getValue();
    }

    /**
    * Close an {@link XMLEventReader}, logging any exceptions that occur as
    * warnings.
    *
    * @param reader
    *            the event reader to close.
    */
    public static void closeQuietly(final XMLEventReader reader) {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
        } catch (XMLStreamException e) {
            LOGGER.warn("Ignored exception closing XMLEventReader.", e);
        }
    }

    /**
    * Close an {@link XMLEventWriter}, logging any exceptions that occur as
    * warnings.
    *
    * @param writer
    *            the event writer to close.
    */
    public static void closeQuietly(final XMLEventWriter writer) {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (final XMLStreamException e) {
            LOGGER.warn("Ignored exception closing XMLEventWriter.", e);
        }
    }

    /** No instances allowed. */
    private OAIXMLUtils() { }
}
