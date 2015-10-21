package org.unizin.cmp.retrieval;

import org.apache.http.impl.client.CloseableHttpClient;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Encapsulates logic for fetching document content based on its metadata.
 *
 * <p>A given {@code Retriever} implementation should contain only a single
 * strategy for retrieval; the correct place to select among different
 * retrieval strategies is in the implementation of
 * {@link RetrievalService#retrieveFileContent(DocumentModel)}.
 * @see RetrievalService
 */
public interface Retriever {
    /**
     * Use {@code httpClient} to download file content associated with the
     * harvested record metadata found on {@code document}.
     * @param httpClient the HTTP client instance to use for retrieval
     * @param document the document specifying what to retrieve
     * @return a transient {@code Blob} containing the downloaded data
     * @throws RetrievalException
     */
    Blob retrieveFileContent(CloseableHttpClient httpClient, DocumentModel document) throws RetrievalException;
}
