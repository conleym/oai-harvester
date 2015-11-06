package org.unizin.cmp.oai.harvester;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.stream.XMLEventFactory;

import org.apache.http.HttpStatus;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.mocks.MockHttpClient;
import org.unizin.cmp.oai.templates.ErrorsTemplate;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import freemarker.template.TemplateException;

public class Tests {
    public static interface FunctionThatThrows {
        void apply() throws Exception;
    }


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

    public static WireMockRule newWireMockRule() {
        return new WireMockRule(WIREMOCK_PORT);
    }

    public static WireMockServer newWireMockServer() {
        final WireMockServer server = new WireMockServer(WIREMOCK_PORT);
        configureFor(WIREMOCK_PORT);
        return server;
    }

    public static void createWiremockStubForOKGetResponse(
            final String responseBody) {
        createWiremockStubForGetResponse(HttpStatus.SC_OK, responseBody);
    }

    public static void createWiremockStubForGetResponse(final int statusCode,
            final String responseBody) {
        createWiremockStubForGetResponse(statusCode, responseBody, ".*");
    }

    public static void createWiremockStubForGetResponse(final int statusCode,
            final String responseBody, final String urlPattern) {
        stubFor(get(urlMatching(urlPattern))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withBody(responseBody)));
    }

    public static void testWithWiremockServer(final FunctionThatThrows test)
            throws Exception {
        final WireMockServer server = newWireMockServer();
        try {
            server.start();
            test.apply();
        } finally {
            server.stop();
        }
    }

    public static void setupWithDefaultError(final MockHttpClient mockClient)
            throws TemplateException, IOException {
        final String arbitraryValidOAIResponse = ErrorsTemplate.process();
        mockClient.addResponseFrom(HttpStatus.SC_OK, "",
                arbitraryValidOAIResponse);
    }


    /** No instances allowed. */
    private Tests() { }
}
