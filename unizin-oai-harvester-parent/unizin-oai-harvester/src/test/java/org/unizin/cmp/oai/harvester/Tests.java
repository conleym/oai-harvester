package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.stream.XMLEventFactory;

import org.unizin.cmp.oai.OAIVerb;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class Tests {
    public static final int DEFAULT_WIREMOCK_PORT = 9000;
    public static final int WIREMOCK_PORT = Integer.parseInt(
            System.getProperty("wiremock.port",
                    String.valueOf(DEFAULT_WIREMOCK_PORT)));

    public static final URI MOCK_OAI_BASE_URI;
    static {
        try {
            MOCK_OAI_BASE_URI = new URI(String.format("http://localhost:%d/oai",
                    WIREMOCK_PORT));
        } catch (final URISyntaxException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static WireMockRule newWireMockRule() {
        return new WireMockRule(WIREMOCK_PORT);
    }

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

    protected static STAX_LIB STAX =
            STAX_LIB.getImplementation();

    public static final OAIVerb DEFAULT_VERB = OAIVerb.LIST_RECORDS;

    public static HarvestParams defaultTestParams() {
        return new HarvestParams(MOCK_OAI_BASE_URI, DEFAULT_VERB);
    }

    public static HarvestParams defaultTestParams(final OAIVerb verb) {
        return new HarvestParams(MOCK_OAI_BASE_URI, verb);
    }
}
