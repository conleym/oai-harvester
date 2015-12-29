package org.unizin.cmp.oai.harvester;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.unizin.cmp.oai.harvester.Tests.STAX;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.OAI2Constants;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.harvester.response.MergingOAIResponseHandler;
import org.unizin.cmp.oai.templates.GetRecordTemplate;
import org.w3c.dom.Document;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public final class TestNonListResponses {
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    @Rule
    public final WireMockRule wireMock = WireMockUtils.newWireMockRule();

    private static final NamespaceContext OAI_CONTEXT = new NamespaceContext(){
        @Override
        public String getNamespaceURI(final String prefix) {
            switch (prefix) {
            case "dc":
                return OAI2Constants.DC_NS_URI;
            case "oai":
                return OAI2Constants.OAI_2_NS_URI;
            case "odc":
                return OAI2Constants.OAI_DC_NS_URI;
            default:
                return null;
            }
        }

        @Override
        public String getPrefix(final String namespaceURI) {
            return null;
        }

        @Override
        public Iterator<?> getPrefixes(final String namespaceURI) {
            return null;
        }
    };

    private static String getRecordXPath(final String suffix) {
        return "/oai:OAI-PMH/oai:GetRecord/oai:record/" + suffix;
    }

    private static String evaluateAsString(final String expression,
            final XPath xpath,
            final Object item) throws XPathExpressionException {
        return (String)xpath.evaluate(expression, item, XPathConstants.STRING);
    }

    @Test
    public void testGetRecord() throws Exception {
        final Instant expectedResponseDate = Instant.now();
        final String expectedIdentifier = "some identifier";
        final String responseBody = new GetRecordTemplate()
                .withIdentifier(expectedIdentifier)
                .withResponseDate(expectedResponseDate)
                .process();
        final List<NameValuePair> parameters = Arrays.asList(
                new BasicNameValuePair("identifier", expectedIdentifier));
        stubFor(post(urlMatching(".*"))
                .withRequestBody(containing(URLEncodedUtils.format(parameters,
                        StandardCharsets.UTF_8)))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(responseBody)));
        final Harvester harvester = new Harvester.Builder()
                .withOAIRequestFactory(PostOAIRequestFactory.getInstance())
                .build();
        final HarvestParams params = new HarvestParams.Builder(
                WireMockUtils.MOCK_OAI_BASE_URI, OAIVerb.GET_RECORD)
                .withIdentifier(expectedIdentifier)
                .build();
        Assert.assertTrue(params.areValid());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        harvester.start(params, new MergingOAIResponseHandler(
                Tests.simpleMergingHandler(out)));

        // This really just tests that we're not messing up the XML.
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setExpandEntityReferences(false);
        final Document doc = dbf.newDocumentBuilder().parse(
                new ByteArrayInputStream(out.toByteArray()));

        final XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(OAI_CONTEXT);

        final String actualIdentifier = evaluateAsString(
                getRecordXPath("oai:header/oai:identifier/text()"), xpath, doc);
        Assert.assertEquals(expectedIdentifier, actualIdentifier);

        final Instant actualResponseDate = Instant.parse(evaluateAsString(
                "/oai:OAI-PMH/oai:responseDate/text()", xpath, doc));
        Assert.assertEquals(expectedResponseDate, actualResponseDate);

        final String coverage = evaluateAsString(
                getRecordXPath("oai:metadata/odc:dc/dc:coverage/text()"),
                xpath, doc);

        /*
         * In Xerces and the JDK, &#13; (carriage return) becomes "\n".
         */
        final String chr13 = (STAX == StAXImplementation.WOODSTOX) ?
                new String(Character.toChars(13)) : "\n";
                Assert.assertEquals("This should have a " + chr13 + " newline.",
                        coverage);
    }
}
