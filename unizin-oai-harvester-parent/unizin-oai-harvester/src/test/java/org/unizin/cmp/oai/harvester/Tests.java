package org.unizin.cmp.oai.harvester;

import java.io.OutputStream;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;

import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.OAIXMLUtils;
import org.unizin.cmp.oai.harvester.response.EventWriterOAIEventHandler;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;

public class Tests {
    public static enum STAX_LIB {
        JDK,
        WOODSTOX,
        XERCES;

        public static STAX_LIB getImplementation() {
            return getImplementationOf(XMLEventFactory.newInstance());
        }

        public static STAX_LIB getImplementationOf(
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
    }

    public static STAX_LIB STAX =
            STAX_LIB.getImplementation();

    public static final OAIVerb DEFAULT_VERB = OAIVerb.LIST_RECORDS;


    public static HarvestParams.Builder defaultTestParams() {
        return new HarvestParams.Builder(WireMockUtils.MOCK_OAI_BASE_URI,
                DEFAULT_VERB);
    }

    public static HarvestParams.Builder defaultTestParams(final OAIVerb verb) {
        return new HarvestParams.Builder(WireMockUtils.MOCK_OAI_BASE_URI, verb);
    }

    public static final OAIEventHandler simpleMergingHandler(
            final OutputStream out)
            throws XMLStreamException {
        final XMLEventWriter eventWriter = OAIXMLUtils.newOutputFactory()
                .createXMLEventWriter(out);
        return new EventWriterOAIEventHandler(eventWriter);
    }


    /** No instances allowed. */
    private Tests() { }
}
