package org.unizin.cmp.retrieval;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Operation(id = CopyFromSourceRepository.ID,
           label = "Copy from source repository",
           description = "Attempt to copy this document's file blob from " +
                         "hrv:sourceRepository to the local repository.")
public class CopyFromSourceRepository  {
    public static final String ID = "UnizinCMP.CopyFromSourceRepository";
    private static final Logger LOG =
            LoggerFactory.getLogger(CopyFromSourceRepository.class);
    public static final String STATUS_PROP = "hrv:retrievalStatus";

    @Context
    private CoreSession session;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {

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
            doc.setPropertyValue(STATUS_PROP, "pending");
            session.saveDocument(doc);
            // Commit change to status property for external visibility
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            RetrievalService retrievalService = Framework.getService(
                    RetrievalService.class);
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            bh.setBlob(retrievalService.retrieveFileContent(doc));
            doc.setPropertyValue(STATUS_PROP, "success");
            session.saveDocument(doc);
            LOG.info("Successfully retrieved {}", doc.getId());
        } catch (NuxeoException e) {
            LOG.warn("Error retrieving {}", doc.getId(), e);
            doc.setPropertyValue(STATUS_PROP,
                                 String.format("failed: %s", e.getMessage()));
            session.saveDocument(doc);
        } finally {
            // set transaction status for later operations
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        return session.getDocument(doc.getRef());
    }
}
