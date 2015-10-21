package org.unizin.cmp.retrieval;

import org.apache.http.impl.client.CloseableHttpClient;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;

public class SuccessRetriever implements Retriever {
    @Override
    public Blob retrieveFileContent(CloseableHttpClient httpClient,
                                    DocumentModel document) throws
            RetrievalException {
        return new ByteArrayBlob(new byte[]{});
    }
}
