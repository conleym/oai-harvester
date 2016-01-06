package org.unizin.cmp.oai.harvester.stax;

import java.util.Objects;

import javax.xml.stream.XMLEventFactory;

/** Enumeration of supported StAX implementations. */
public enum StAXImplementation {
    AALTO("com.fasterxml.aalto.stax.InputFactoryImpl",
            "com.fasterxml.aalto.stax.OutputFactoryImpl",
            "com.fasterxml.aalto.stax.EventFactoryImpl"),
    /** JDK built in implementation, based on Xerces. */
    JDK("com.sun.xml.internal.stream.XMLInputFactoryImpl",
        "com.sun.xml.internal.stream.XMLOutputFactoryImpl",
        "com.sun.xml.internal.stream.events.XMLEventFactoryImpl"),
    WOODSTOX("com.ctc.wstx.stax.WstxInputFactory",
             "com.ctc.wstx.stax.WstxOutputFactory",
             "com.ctc.wstx.stax.WstxEventFactory"),
    XERCES(JDK.inputFactory,
           JDK.outputFactory,
           "org.apache.xerces.stax.XMLEventFactoryImpl");

    /**
     * Get the current implementation.
     *
     * @return the current StAX implementation, or {@code null} if the current
     *         implementation is not supported or cannot be determined.
     */
    public static StAXImplementation getImplementation() {
        return getImplementationOf(XMLEventFactory.newInstance());
    }

    public static StAXImplementation getImplementationOf(
            final XMLEventFactory inputFactory) {
        final String className = inputFactory.getClass().getName();
        for (final StAXImplementation impl : values()) {
            if (Objects.equals(className, impl.eventFactory)) {
                return impl;
            }
        }
        return null;
    }

    /**
     * The StAX implementation that would be used by default in this particular
     * runtime environment.
     */
    public static StAXImplementation DEFAULT = getImplementation();

    final String inputFactory;
    final String outputFactory;
    final String eventFactory;

    private StAXImplementation(final String inputFactory,
            final String outputFactory, final String eventFactory) {
        this.inputFactory = inputFactory;
        this.outputFactory = outputFactory;
        this.eventFactory = eventFactory;
    }

    /**
     * Tell the JDK to use this StAXImplementation.
     */
    public void use() {
        System.setProperty("javax.xml.stream.XMLInputFactory", inputFactory);
        System.setProperty("javax.xml.stream.XMLOutputFactory", outputFactory);
        System.setProperty("javax.xml.stream.XMLEventFactory", eventFactory);
    }

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
