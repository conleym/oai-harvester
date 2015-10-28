package org.unizin.cmp.retrieval;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

public class BlobAttacher extends UnrestrictedSessionRunner {
    private final DocumentRef docRef;
    private final Blob blob;

    public BlobAttacher(CoreSession session, DocumentRef doc, Blob blob) {
        super(session);
        this.docRef = doc;
        this.blob = blob;
    }

    @Override
    public void run() {
        DocumentModel doc = session.getSourceDocument(docRef);
        if (doc != null) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            bh.setBlob(blob);
            session.saveDocument(doc);
        }
    }
}
