package org.unizin.cmp.retrieval;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

import java.io.IOException;
import java.io.InputStream;

public class RetrievalRepositoryInit extends DefaultRepositoryInit {
    @Override
    public void populate(CoreSession session) {
        super.populate(session);
        InputStream archiveStream = getClass().getResourceAsStream("/testdoc.zip");
        try {
            DocumentModel testDoc =
                    RetrievalTestUtils.createTestDoc(archiveStream, session);
            DocumentModel testDoc2 = session.createDocumentModel("/", "testdoc2", "File");
            testDoc2.copyContent(testDoc);
            testDoc2.setPropertyValue("hrv:sourceRepository",
                                      "http://example.com/oai");
            session.createDocument(testDoc2);
            DocumentModel testDoc3 = session.createDocumentModel("/", "testdoc3", "File");
            testDoc3.copyContent(testDoc);
            testDoc3.setPropertyValue("hrv:retrievalStatus", "pending");
            session.createDocument(testDoc3);
            DocumentModel testDoc4 = session.createDocumentModel("/", "testdoc4", "File");
            testDoc4.copyContent(testDoc);
            session.createDocument(testDoc4);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
