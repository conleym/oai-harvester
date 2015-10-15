package org.unizin.cmp.retrieval;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.ByteStreams;
import org.hamcrest.Matchers;
import org.junit.Before;
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
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.*;
import static org.unizin.cmp.retrieval.CopyFromSourceRepository.*;


@RunWith(FeaturesRunner.class)
@Features({TransactionalFeature.class, AutomationFeature.class, PlatformFeature.class})
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({"org.unizin.cmp.schemas", "org.unizin.cmp.retrieval"})
public class CopyFromSourceRepositoryTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9231);

    @Inject
    AutomationService automationService;

    @Inject
    CoreSession session;

    @Inject
    WorkManager workManager;

    DocumentModel inputDoc;
    byte[] pdfResponseBody;

    DocumentModel createTestDoc() throws IOException {
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

    @Before
    public void setUp() throws Exception {
        inputDoc = createTestDoc();
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
    }

    @Test
    public void testCopyFromSourceRepository() throws OperationException,
            IOException {
        OperationContext context = new OperationContext(session);
        context.setInput(inputDoc);
        OperationChain chain = new OperationChain("testCopyFromSourceRepository");
        chain.add(ID);
        DocumentModel outputDoc =
                (DocumentModel) automationService.run(context, chain);
        BlobHolder bh = outputDoc.getAdapter(BlobHolder.class);
        Blob blob = bh.getBlob();
        assertNotNull(blob);
        assertArrayEquals(pdfResponseBody, blob.getByteArray());
        assertEquals("success", outputDoc.getPropertyValue(STATUS_PROP));
    }

    @Test
    public void testRetrieveCopyFromSourceRepository() throws
            OperationException, IOException, InterruptedException {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        OperationContext context = new OperationContext(session);
        context.setInput(inputDoc);
        OperationChain chain = new OperationChain("testRetrieveCopyFromSourceRepository");
        chain.add(RetrieveCopyFromSourceRepository.ID);
        DocumentModel outputDoc =
                (DocumentModel) automationService.run(context, chain);
        workManager.awaitCompletion(10, TimeUnit.SECONDS);
        BlobHolder bh = outputDoc.getAdapter(BlobHolder.class);
        Blob blob = bh.getBlob();
        assertNotNull(blob);
        assertArrayEquals(pdfResponseBody, blob.getByteArray());
    }

    @Test
    public void testNoSimultaneousDownloads() throws
            OperationException, InterruptedException, IOException {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        inputDoc.setPropertyValue(STATUS_PROP, "pending");
        session.saveDocument(inputDoc);
        OperationContext context = new OperationContext(session);
        context.setInput(inputDoc);
        OperationChain chain = new OperationChain("testNoSimultaneousDownloads");
        chain.add(RetrieveCopyFromSourceRepository.ID);
        DocumentModel outputDoc =
                (DocumentModel) automationService.run(context, chain);
        workManager.awaitCompletion(10, TimeUnit.SECONDS);
        BlobHolder bh = outputDoc.getAdapter(BlobHolder.class);
        Blob blob = bh.getBlob();
        assertNull(blob);
    }

    @Test
    public void testFailure() throws OperationException {
        OperationContext context = new OperationContext(session);
        inputDoc.setPropertyValue("hrv:identifier", new String[] {"http://localhost:9231/nonexistent"});
        session.saveDocument(inputDoc);
        context.setInput(inputDoc);
        OperationChain chain = new OperationChain("testFailure");
        chain.add(ID);
        DocumentModel outputDoc =
                (DocumentModel) automationService.run(context, chain);
        assertThat((String) outputDoc.getPropertyValue(STATUS_PROP),
                   Matchers.startsWith("failed"));
    }
}
