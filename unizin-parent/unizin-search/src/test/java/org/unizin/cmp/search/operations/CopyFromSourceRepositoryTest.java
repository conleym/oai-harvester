package org.unizin.cmp.search.operations;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.ByteStreams;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
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
@Features(AutomationFeature.class)
@Deploy({"org.unizin.cmp.schemas", "org.unizin.cmp.search"})
public class CopyFromSourceRepositoryTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9231);

    @Inject
    AutomationService automationService;

    @Inject
    CoreSession session;

    private DocumentModel createTestDoc() throws IOException {
        InputStream archiveStream = getClass().getResourceAsStream("/testdoc.zip");
        DocumentReader reader = new NuxeoArchiveReader(archiveStream);
        DocumentWriter writer = new DocumentModelWriter(session, "/");
        DocumentPipe pipe = new DocumentPipeImpl();
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();
        session.save();
        return session.getDocument(new PathRef("/Untitled.1444280484660"));
    }

    @Test
    public void testCopyFromSourceRepository() throws
        OperationException,
        IOException {
        byte[] htmlResponseBody = ByteStreams.toByteArray(
            getClass().getResourceAsStream("/testdocresponse.html"));
        byte[] pdfResponseBody = ByteStreams.toByteArray(
            getClass().getResourceAsStream(("/testdocpage8.pdf")));
        stubFor(get(urlEqualTo("/2027/loc.ark:/13960/t6252864g"))
                    .willReturn(aResponse().withStatus(303).withHeader(
                        "Location", "http://localhost:9231/htmlout")));
        stubFor(get(urlEqualTo("/htmlout"))
                    .willReturn(aResponse()
                                    .withHeader("Content-Type", "text/html; charset=utf-8")
                                    .withBody(htmlResponseBody)));
        stubFor(get(urlEqualTo("/cgi/imgsrv/download/pdf?id=loc.ark%3A%2F13960%2Ft6252864g;orient=0;size=100"))
                    .willReturn(aResponse()
                                    .withHeader("Content-Type",
                                                "application/pdf")
                                    .withBody(pdfResponseBody)));
        DocumentModel inputDoc = createTestDoc();
        OperationContext context = new OperationContext(session);
        context.setInput(inputDoc);
        OperationChain chain = new OperationChain("testCopyFromSourceRepository");
        chain.add(CopyFromSourceRepository.ID);
        DocumentModel outputDoc =
            (DocumentModel) automationService.run(context, chain);
        BlobHolder bh = outputDoc.getAdapter(BlobHolder.class);
        Blob blob = bh.getBlob();
        assertNotNull(blob);
        assertArrayEquals(pdfResponseBody, blob.getByteArray());
    }
}
