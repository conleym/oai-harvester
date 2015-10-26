package org.unizin.cmp.retrieval;

import org.hamcrest.Matchers;
import org.junit.Before;
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
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.TransactionalFeature;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.unizin.cmp.retrieval.CopyFromSourceRepository.ID;
import static org.unizin.cmp.retrieval.CopyFromSourceRepository.STATUS_PROP;


@RunWith(FeaturesRunner.class)
@Features({TransactionalFeature.class, AutomationFeature.class, PlatformFeature.class})
@Deploy({"org.unizin.cmp.schemas", "org.unizin.cmp.retrieval",
         "org.unizin.cmp.retrieval.tests:test-retrieval-contrib.xml"})
public class CopyFromSourceRepositoryTest {

    @Inject
    AutomationService automationService;

    @Inject
    CoreSession session;

    @Inject
    WorkManager workManager;

    private DocumentModel inputDoc;

    @Before
    public void setUp() throws Exception {
        InputStream archiveStream = getClass().getResourceAsStream("/testdoc.zip");
        inputDoc = RetrievalTestUtils.createTestDoc(archiveStream, session);
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
        DocumentModel outputDoc = (DocumentModel) automationService.run(context, chain);

        // check that operation is asynchronous (not visible in current transaction)
        assertEquals("pending", outputDoc.getPropertyValue(STATUS_PROP));
        BlobHolder bh = outputDoc.getAdapter(BlobHolder.class);
        assertNull(bh.getBlob());

        // check that operation works
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        workManager.awaitCompletion(10, TimeUnit.SECONDS);
        outputDoc = session.getDocument(inputDoc.getRef());
        bh = outputDoc.getAdapter(BlobHolder.class);
        assertNotNull(bh.getBlob());
    }

    @Test
    public void testNoSimultaneousDownloads() throws
            OperationException, InterruptedException, IOException {
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

        inputDoc.setPropertyValue("hrv:sourceRepository", "http://example.com/oai");
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
