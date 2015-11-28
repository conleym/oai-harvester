package org.unizin.cmp.oai.harvester;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventFactory;

import org.apache.http.HttpStatus;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.templates.ErrorsTemplate;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import freemarker.template.TemplateException;

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

    private static final String LITERAL_QM = Pattern.quote("?");

    public static final String URL_PATTERN_WITHOUT_RESUMPTION_TOKEN =
            "^.*\\?(?:(?!resumptionToken).)*$";

    public static String urlResmptionTokenPattern(
            final String resumptionToken) {
        return "^.*" + LITERAL_QM + ".*resumptionToken=" +
                Pattern.quote(resumptionToken) + ".*$";
    }

    public static HarvestParams.Builder defaultTestParams() {
        return new HarvestParams.Builder(MOCK_OAI_BASE_URI, DEFAULT_VERB);
    }

    public static HarvestParams.Builder defaultTestParams(final OAIVerb verb) {
        return new HarvestParams.Builder(MOCK_OAI_BASE_URI, verb);
    }

    public static WireMockRule newWireMockRule() {
        return new WireMockRule(WIREMOCK_PORT);
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

    public static String setupWithDefaultError()
            throws TemplateException, IOException {
        final String defaultError = ErrorsTemplate.process();
        createWiremockStubForOKGetResponse(defaultError);
        return defaultError;
    }


    /** No instances allowed. */
    private Tests() { }
}
