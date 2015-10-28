package org.unizin.cmp.retrieval;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Nuxeo service interface for retrieving document content via HTTP.
 */
public interface RetrievalService {
    /**
     * Retrieve content specified by the metadata on {@code document}.
     *
     * <p>Note that this method only returns a Blob and is not intended to change
     * the input document or the state of the repository; attaching the content
     * and committing the changes is the responsibility of calling code.
     *
     * @param document the document with metadata specifying the location of
     *                 remote content
     * @return the content associated with {@code document}
     */
    Blob retrieveFileContent(DocumentModel document) throws RetrievalException;
}
