package org.unizin.cmp.retrieval;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.ByteStreams;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;
import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.unizin.cmp.retrieval.RetrievalTestUtils.replacePort;


@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({"org.unizin.cmp.schemas", "org.unizin.cmp.retrieval"})
public class OreRecordRetrieverTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(
            Integer.parseInt(System.getProperty("unizin.test.wiremock.port")));

    @Inject
    CoreSession session;

    private DocumentModel inputDoc;
    private byte[] pdfResponseBody;
    private String port;

    @Before
    public void setUp() throws IOException {
        port = System.getProperty("unizin.test.wiremock.port");
        byte[] xmlResponseBody = replacePort(
                getClass().getResourceAsStream("/ore-test-response.xml"), port);
        pdfResponseBody = ByteStreams.toByteArray(
                getClass().getResourceAsStream("/testdocpage8.pdf"));
        stubFor(get(urlEqualTo("/dspace/bitstream/handle/1811/20/Halm_OLN.pdf?sequence=4"))
                        .willReturn(aResponse().withHeader("Content-Type", "application/pdf")
                                            .withBody(pdfResponseBody)));
        stubFor(get(urlPathEqualTo("/oai/request"))
                        .withQueryParam("verb", equalTo("GetRecord"))
                        .withQueryParam("identifier",
                                        equalTo("oai%3Akb.osu.edu%3A1811%2F20"))
                        .withQueryParam("metadataPrefix", equalTo("ore"))
                        .willReturn(aResponse().withHeader("Content-Type",
                                                           "application/xml")
                                            .withBody(xmlResponseBody)));

        inputDoc = session.createDocumentModel("/", "testdoc", "File");
        inputDoc.addFacet("Harvested");
        inputDoc.setPropertyValue("hrv:oaiIdentifier", "oai:kb.osu.edu:1811/20");
        inputDoc.setPropertyValue("hrv:sourceRepository",
                                  "http://localhost:" + port + "/oai/request");
        inputDoc = session.createDocument(inputDoc);
    }

    @Test
    public void testOreRetriever() throws IOException {
        OreRecordRetriever retriever = new OreRecordRetriever();
        CloseableHttpClient client = HttpClients.createDefault();
        Blob result = retriever.retrieveFileContent(client, inputDoc);
        assertNotNull(result);
        assertArrayEquals(pdfResponseBody, result.getByteArray());
    }

}
