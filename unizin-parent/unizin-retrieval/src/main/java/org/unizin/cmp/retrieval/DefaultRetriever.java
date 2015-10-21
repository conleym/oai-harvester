package org.unizin.cmp.retrieval;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

import java.io.IOException;

/**
 * Default implementation of a {@link Retriever}.
 *
 */
public class DefaultRetriever implements Retriever {

    protected static final String IDENTIFIER_PROP = "hrv:identifier";
    protected static final String DEFAULT_RETRIEVED_FILENAME = "retrievedFile";

    /**
     * A default implementation that always raises an error.
     *
     * @param httpClient the HTTP client instance to use for retrieval
     * @param document the document specifying what to retrieve
     * @return
     * @throws RetrievalException
     */
    @Override
    public Blob retrieveFileContent(CloseableHttpClient httpClient, DocumentModel document) throws
            RetrievalException {
        throw new UnsupportedOperationException(
                "retrieveFileContent must be overridden in subclass");
    }

    /**
     * Return a {@link org.nuxeo.ecm.core.api.Blob} from {@code entity} that
     * has the content and content type of the entity, and a file name equal to
     * {@link DefaultRetriever#DEFAULT_RETRIEVED_FILENAME}.
     *
     * <p>This blob should not be considered to be in durable storage; it
     * should be added to (for example) an
     * {@link org.nuxeo.ecm.core.api.blobholder.BlobHolder} by the code
     * that eventually receives its results.
     *
     * @param entity an entity from an {@link org.apache.http.HttpResponse}
     * @return the contents of {@code entity} as a {@link org.nuxeo.ecm.core.api.Blob}
     * @throws IOException
     */
    protected final Blob blobFromEntity(HttpEntity entity) throws IOException {
        if (entity == null) {
            throw new RetrievalException("no content found");
        }
        FileBlob blob = new FileBlob(entity.getContent());
        blob.setMimeType(ContentType.getOrDefault(entity).getMimeType());
        blob.setFilename(DEFAULT_RETRIEVED_FILENAME);
        return blob;
    }
}
