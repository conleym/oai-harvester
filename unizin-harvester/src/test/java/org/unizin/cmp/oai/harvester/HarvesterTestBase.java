package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.stream.XMLEventFactory;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.mocks.MockHttpClient;


public class HarvesterTestBase {
    protected static final Logger LOGGER = LoggerFactory.getLogger(
            "harvester-tests");

    public static final OAIVerb DEFAULT_VERB = OAIVerb.LIST_RECORDS;

    public static final URI TEST_URI;
    static {
        try {
            TEST_URI = new URI("http://test/oai");
        } catch (final URISyntaxException e) {
            throw new ExceptionInInitializerError(e);
        }
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
                    LOGGER.warn(
                        "Unrecognized StAX XMLEventFactory implementation: {}.",
                        classname);
                    return null;
            }
        }
    }

    protected static STAX_LIB STAX =
            STAX_LIB.getImplementation();


    public static HarvestParams defaultTestParams() {
        return new HarvestParams(TEST_URI, DEFAULT_VERB);
    }

    public static HarvestParams defaultTestParams(final OAIVerb verb) {
        return new HarvestParams(TEST_URI, verb);
    }


    protected MockHttpClient mockHttpClient;

    @Before
    public void initMockClient() {
        mockHttpClient = new MockHttpClient();
    }

    protected Harvester defaultTestHarvester() {
        final Harvester harvester = new Harvester.Builder()
                .withHttpClient(mockHttpClient)
                .build();
        return harvester;
    }

    protected HarvesterTestBase() {}
}
