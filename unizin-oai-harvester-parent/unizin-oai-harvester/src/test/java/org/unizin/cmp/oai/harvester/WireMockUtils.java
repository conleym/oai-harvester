package org.unizin.cmp.oai.harvester;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.unizin.cmp.oai.templates.ErrorsTemplate;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
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


    public static MappingBuilder getAnyURL() {
        return get(urlMatching(".*"));
    }

    public static MappingBuilder postAnyURL() {
        return post(urlMatching(".*"));
    }

    public static MappingBuilder matchingPostParams(final MappingBuilder mb,
            final Map<String, String> params) {
        params.forEach((k,v) -> {
            final NameValuePair nvp = new BasicNameValuePair(k, v);
            final String enc = URLEncodedUtils.format(
                    Collections.singleton(nvp), StandardCharsets.UTF_8);
            mb.withRequestBody(containing(enc));
        });
        return mb;
    }

    private static String urlEncode(final String string) {
        try {
            return URLEncoder.encode(string, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException e) {
            // Can't happen in standards-compliant JDKs.
            throw new RuntimeException(e);
        }
    }

    public static MappingBuilder matchingQueryParams(final MappingBuilder mb,
            final Map<String, String> params) {
        params.forEach((k, v) -> mb.withQueryParam(urlEncode(k),
                    equalTo(urlEncode(v))));
        return mb;
    }

    public static MappingBuilder returning(final MappingBuilder mb,
            final int statusCode) {
        return returning(mb, statusCode, "");
    }

    public static MappingBuilder returning(final MappingBuilder mb,
            final int statusCode, final String responseBody) {
        mb.willReturn(aResponse()
                .withStatus(statusCode)
                .withBody(responseBody));
        return mb;
    }


    public static String oaiErrorResponseStub()
            throws TemplateException, IOException {
        final String defaultError = ErrorsTemplate.process();
        WireMockUtils.getStub(defaultError);
        return defaultError;
    }

    public static void getStub(final int statusCode,
            final String responseBody, final String urlPattern) {
        final MappingBuilder mb = get(urlMatching(urlPattern));
        returning(mb, statusCode, responseBody);
        stubFor(mb);
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
