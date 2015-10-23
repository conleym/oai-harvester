package org.unizin.cmp.retrieval;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.ByteStreams;
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
import java.io.InputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({"org.unizin.cmp.schemas", "org.unizin.cmp.retrieval"})
public class RetrievalServiceTest {


    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9231);

    @Inject
    RetrievalService retrievalService;

    @Inject
    CoreSession session;

    private DocumentModel inputDoc;
    private byte[] pdfResponseBody;

    @Test
    public void testRegistration() {
        assertNotNull(retrievalService);
    }

    @Before
    public void setUp() throws IOException {
        byte[] htmlResponseBody = ByteStreams.toByteArray(
                getClass().getResourceAsStream("/testdocresponse.html"));
        pdfResponseBody = ByteStreams.toByteArray(
                getClass().getResourceAsStream(("/testdocpage8.pdf")));
        stubFor(get(urlEqualTo("/2027/loc.ark:/13960/t6252864g"))
                        .willReturn(aResponse().withStatus(303).withHeader(
                                "Location", "http://localhost:9231/htmlout")));
        stubFor(get(urlEqualTo("/htmlout"))
                        .willReturn(aResponse().withHeader(
                                "Content-Type", "text/html; charset=utf-8")
                                            .withBody(htmlResponseBody)));
        stubFor(get(urlEqualTo("/cgi/imgsrv/download/pdf?id=loc.ark%3A%2F13960%2Ft6252864g;orient=0;size=100"))
                        .willReturn(aResponse().withHeader(
                                "Content-Type", "application/pdf")
                                            .withBody(pdfResponseBody)));
        InputStream archiveStream = getClass().getResourceAsStream("/testdoc.zip");
        inputDoc = RetrievalTestUtils.createTestDoc(archiveStream, session);
    }

    @Test
    public void testContribution() throws IOException {
        Blob result = retrievalService.retrieveFileContent(inputDoc);
        assertNotNull(result);
        assertArrayEquals(pdfResponseBody, result.getByteArray());
    }

    @Test(expected=RetrievalException.class)
    public void testFailure()  {
        inputDoc.setPropertyValue("hrv:identifier", new String[] {"http://localhost:9231/nonexistent"});
        retrievalService.retrieveFileContent(inputDoc);
    }
}
