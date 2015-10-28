package org.unizin.cmp.retrieval;

import org.hamcrest.Matchers;
import org.junit.After;
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
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
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
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.unizin.cmp.retrieval.CopyFromSourceRepository.ID;
import static org.unizin.cmp.retrieval.CopyFromSourceRepository.STATUS_PROP;


@RunWith(FeaturesRunner.class)
@Features({TransactionalFeature.class, AutomationFeature.class, PlatformFeature.class})
@Deploy({"org.unizin.cmp.schemas", "org.unizin.cmp.retrieval",
         "org.unizin.cmp.retrieval.tests:test-retrieval-contrib.xml"})
@RepositoryConfig(init = RetrievalRepositoryInit.class, cleanup = Granularity.METHOD)
public class CopyFromSourceRepositoryTest {

    @Inject
    AutomationService automationService;

    @Inject
    CoreFeature coreFeature;

    CoreSession session;

    @Inject
    WorkManager workManager;

    private DocumentModel inputDoc;

    @Before
    public void setUp() {
        try (CoreSession sysSession = coreFeature.openCoreSession(
                SecurityConstants.SYSTEM_USERNAME)) {
            DocumentModel root = sysSession.getRootDocument();
            ACP acp = root.getACP();
            ACL acl = acp.getOrCreateACL();
            acl.add(new ACE("unprivileged", READ, true));
            acp.addACL(acl);
            root.setACP(acp, true);
            sysSession.save();
        }
        session = coreFeature.openCoreSession("unprivileged");
    }

    @After
    public void tearDown() {
        session.close();
    }

    @Test
    public void testCopyFromSourceRepository() throws OperationException,
            IOException, LoginException {
        inputDoc = session.getDocument(new PathRef("Untitled.1444280484660"));
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
            OperationException,
            IOException,
            InterruptedException,
            LoginException {
        inputDoc = session.getDocument(new PathRef("/testRetrieveCopyFromSourceRepository"));
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
        outputDoc = session.getDocument(outputDoc.getRef());
        bh = outputDoc.getAdapter(BlobHolder.class);
        assertNotNull(bh.getBlob());
    }

    @Test
    public void testNoSimultaneousDownloads() throws
            OperationException,
            InterruptedException,
            IOException,
            LoginException {
        inputDoc = session.getDocument(new PathRef("/testNoSimultaneousDownloads"));
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
    public void testFailure() throws OperationException, LoginException {
        inputDoc = session.getDocument(new PathRef("/testFailure"));
        OperationContext context = new OperationContext(session);
        context.setInput(inputDoc);
        OperationChain chain = new OperationChain("testFailure");
        chain.add(ID);
        DocumentModel outputDoc =
                (DocumentModel) automationService.run(context, chain);
        assertThat((String) outputDoc.getPropertyValue(STATUS_PROP),
                   Matchers.startsWith("failed"));
    }
}
