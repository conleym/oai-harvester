package org.unizin.cmp.retrieval;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

public class RetrievalStatusPropertySetter extends UnrestrictedSessionRunner {

    public static final String STATUS_PROP = "hrv:retrievalStatus";

    private final String newValue;
    private final DocumentRef docRef;

    public RetrievalStatusPropertySetter(CoreSession session, DocumentRef docRef, String newValue) {
        super(session);
        this.docRef = docRef;
        this.newValue = newValue;
    }

    @Override
    public void run() {
        DocumentModel doc = session.getSourceDocument(docRef);
        if (doc != null) {
            doc.setPropertyValue(STATUS_PROP, newValue);
            session.saveDocument(doc);
        }
    }
}
