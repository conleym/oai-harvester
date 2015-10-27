package org.unizin.cmp.retrieval;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;

@Operation(id = CopyFromSourceRepository.ID,
           label = "Copy from source repository",
           description = "Attempt to copy this document's file blob from " +
                         "hrv:sourceRepository to the local repository.")
public class CopyFromSourceRepository {
    public static final String ID = "UnizinCMP.CopyFromSourceRepository";
    private static final Logger LOG =
            LoggerFactory.getLogger(CopyFromSourceRepository.class);
    public static final String STATUS_PROP = "hrv:retrievalStatus";

    @Context
    private CoreSession session;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws LoginException {

        if (!doc.hasFacet("Harvested")) {
            LOG.warn("{} called on document without Harvested facet", ID);
            return doc;
        }
        // Commit any existing transactions (in case in operation chain or Work)
        // see http://tinyurl.com/o4p3nyp
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        try {
            doc = session.getDocument(doc.getRef());
            new RetrievalStatusPropertySetter(session, doc.getRef(), "pending").runUnrestricted();
            // Commit change to status property for external visibility
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            RetrievalService retrievalService = Framework.getService(
                    RetrievalService.class);
            Blob result = retrievalService.retrieveFileContent(doc);
            new BlobAttacher(session, doc.getRef(), result).runUnrestricted();
            new RetrievalStatusPropertySetter(session, doc.getRef(), "success").runUnrestricted();
            LOG.info("Successfully retrieved {}", doc.getId());
        } catch (NuxeoException e) {
            LOG.error("Error retrieving {}", doc.getId(), e);
            String msg = String.format("failed: %s", e.getMessage());
            new RetrievalStatusPropertySetter(session, doc.getRef(), msg).runUnrestricted();
        } finally {
            // set transaction status for later operations
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        return session.getDocument(doc.getRef());
    }
}
