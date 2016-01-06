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

    /**
     * Create a new {@code XMLInputFactory} instance.
     * <p>
     * The new instance will be namespace aware, and will not
     * support DTDs (see below).
     * </p>
     *
     * <h2>Security Considerations</h2>
     *
     * <p>
     * The new instance will <em>not</em> support DTDs (meaning it also doesn't
     * support external entities). See <a href=
     * "https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Processing">
     * here</a> for info about external entity and DTD-based attacks.
     * </p>
     *
     * @return a new input factory instance with the settings as described
     *         above.
     */
    public static XMLInputFactory newInputFactory() {
        final XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
                false);
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
