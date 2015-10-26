package org.unizin.cmp.oai.harvester;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.OAI2Constants;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.harvester.response.MergingOAIResponseHandler;
import org.unizin.cmp.oai.templates.GetRecordTemplate;
import org.w3c.dom.Document;

public final class TestNonListResponses extends HarvesterTestBase {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

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
        final String responseContent = new GetRecordTemplate()
                .withIdentifier(expectedIdentifier)
                .withResponseDate(expectedResponseDate)
                .process();
        mockHttpClient.addResponseFrom(HttpStatus.SC_OK, "",
                responseContent);
        final Harvester harvester = new Harvester.Builder()
                .withHttpClient(mockHttpClient)
                .withOAIRequestFactory(PostOAIRequestFactory.getInstance())
                .build();
        final HarvestParams params = new HarvestParams(TEST_URI,
                OAIVerb.GET_RECORD)
                .withIdentifier(expectedIdentifier);
        Assert.assertTrue(params.areValid());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        harvester.start(params, new MergingOAIResponseHandler(out));

        // This really just tests that we're not messing up the XML.
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
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
        String chr13 = (STAX == STAX_LIB.WOODSTOX) ?
                new String(Character.toChars(13)) : "\n";
        Assert.assertEquals("This should have a " + chr13 + " newline.",
                coverage);
    }
}
