package org.unizin.cmp.retrieval;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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

    private final Blob blobFromEntity(HttpEntity entity) throws IOException {
        if (entity == null) {
            throw new RetrievalException("no content found");
        }
        FileBlob blob = new FileBlob(entity.getContent());
        blob.setMimeType(ContentType.getOrDefault(entity).getMimeType());
        blob.setFilename(DEFAULT_RETRIEVED_FILENAME);
        return blob;
    }

    /**
     * Return a {@link org.nuxeo.ecm.core.api.Blob} with the contents of
     * {@code dataUrl} as retrieved by {@code client}.
     *
     * <p>This blob should not be considered to be in durable storage; it
     * should be added to (for example) an
     * {@link org.nuxeo.ecm.core.api.blobholder.BlobHolder} by the code
     * that eventually receives its results.
     *
     * @param client an HTTP client to use for retrieval
     * @param dataUrl the URL containing the blob's content
     * @return the contents of {@code dataUrl} as a {@link org.nuxeo.ecm.core.api.Blob}
     */
    protected final Blob retrieveContent(CloseableHttpClient client,
                                         String dataUrl) {
        URI uri;
        try {
            uri = new URI(dataUrl);
        } catch (URISyntaxException e) {
            throw new RetrievalException(e);
        }
        HttpGet req = new HttpGet(uri);
        try (CloseableHttpResponse resp = client.execute(req)) {
            if (resp.getStatusLine().getStatusCode() != 200) {
                throw new RetrievalException(resp.getStatusLine().getReasonPhrase());
            }
            return blobFromEntity(resp.getEntity());
        } catch (IOException e) {
            throw new RetrievalException(e);
        }
    }
}
