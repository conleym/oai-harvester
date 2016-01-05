package org.unizin.cmp.oai.harvester;

import javax.xml.stream.XMLEventFactory;

public enum StAXImplementation {
    JDK,
    WOODSTOX,
    XERCES;

    public static StAXImplementation getImplementation() {
        return getImplementationOf(XMLEventFactory.newInstance());
    }

    public static StAXImplementation getImplementationOf(
            final XMLEventFactory inputFactory) {
        final String classname = inputFactory.getClass().getName();
        switch (classname) {
        case "com.sun.xml.internal.stream.events.XMLEventFactoryImpl":
            return JDK;
        case "com.ctc.wstx.stax.WstxEventFactory":
            return WOODSTOX;
        case "org.apache.xerces.stax.XMLEventFactoryImpl":
            return XERCES;
        default:
            return null;
        }
    }

    public static StAXImplementation DEFAULT = getImplementation();

    /**
     * The JDK implementation is based on Xerces. Thus, they can be expected to
     * perform similarly in many cases, so it can be useful to know if a given
     * implementation is either of these.
     *
     * @return {@code true} iff this instance represents the JDK's built in
     *         implementation or Xerces.
     */
    public boolean isJDKOrXerces() {
        return this == JDK || this == XERCES;
    }
}
