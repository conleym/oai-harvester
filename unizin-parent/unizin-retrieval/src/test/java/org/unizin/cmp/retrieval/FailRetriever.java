package org.unizin.cmp.retrieval;

import org.apache.http.impl.client.CloseableHttpClient;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

public class FailRetriever implements Retriever {
    @Override
    public Blob retrieveFileContent(CloseableHttpClient httpClient,
                                    DocumentModel document) throws
            RetrievalException {
        throw new RetrievalException("always fails");
    }
}
