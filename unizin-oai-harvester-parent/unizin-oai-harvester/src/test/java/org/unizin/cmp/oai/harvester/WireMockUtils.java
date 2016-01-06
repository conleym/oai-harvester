package org.unizin.cmp.oai.harvester;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpStatus;
import org.unizin.cmp.oai.templates.ErrorsTemplate;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import freemarker.template.TemplateException;

public final class WireMockUtils {

    public static final int DEFAULT_WIREMOCK_PORT = 9000;
    public static final int WIREMOCK_PORT = Integer.parseInt(
            System.getProperty("wiremock.port",
                    String.valueOf(DEFAULT_WIREMOCK_PORT)));

    public static final URI MOCK_OAI_BASE_URI;
    static {
        try {
            MOCK_OAI_BASE_URI = new URI(String.format("http://localhost:%d/oai",
                    WireMockUtils.WIREMOCK_PORT));
        } catch (final URISyntaxException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static WireMockRule newWireMockRule() {
        return new WireMockRule(WIREMOCK_PORT);
    }

    public static String oaiErrorResponse()
            throws TemplateException, IOException {
        final String defaultError = ErrorsTemplate.process();
        WireMockUtils.getStub(defaultError);
        return defaultError;
    }

    public static void getStub(final int statusCode,
            final String responseBody, final String urlPattern) {
        stubFor(get(urlMatching(urlPattern))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withBody(responseBody)));
    }

    public static void getStub(final int statusCode,
            final String responseBody) {
        getStub(statusCode, responseBody, ".*");
    }

    public static void getStub(
            final String responseBody) {
        getStub(HttpStatus.SC_OK, responseBody);
    }


    /** No instances allowed. */
    private WireMockUtils() { }
}
