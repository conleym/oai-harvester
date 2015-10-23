package org.unizin.cmp.retrieval;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

import java.io.IOException;

/**
 * Abstract implementation of a {@link Retriever}.
 *
 */
public abstract class BaseRetriever implements Retriever {

    protected static final String IDENTIFIER_PROP = "hrv:identifier";
    protected static final String DEFAULT_RETRIEVED_FILENAME = "retrievedFile";

    @Override
    public abstract Blob retrieveFileContent(CloseableHttpClient httpClient,
                                             DocumentModel document)
            throws RetrievalException;

    /**
     * Return a {@link org.nuxeo.ecm.core.api.Blob} from {@code entity} that
     * has the content and content type of the entity, and a file name equal to
     * {@link BaseRetriever#DEFAULT_RETRIEVED_FILENAME}.
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
