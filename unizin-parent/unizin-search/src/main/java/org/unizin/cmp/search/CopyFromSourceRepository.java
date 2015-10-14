package org.unizin.cmp.search;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Operation(id = CopyFromSourceRepository.ID,
           label = "Copy from source repository",
           description = "Attempt to copy this document's file blob from " +
                         "hrv:sourceRepository to the local repository.")
public class CopyFromSourceRepository  {
    public static final String ID = "UnizinCMP.CopyFromSourceRepository";
    public static final Logger LOG =
        LoggerFactory.getLogger(CopyFromSourceRepository.class);
    public static final String STATUS_PROP = "hrv:retrievalStatus";

    @Context
    CoreSession session;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        doc = session.getDocument(doc.getRef());
        if (!doc.hasFacet("Harvested")) {
            LOG.warn("{} called on document without Harvested facet", ID);
            return doc;
        }
        doc.setPropertyValue(STATUS_PROP, "pending");
        session.saveDocument(doc);
        List<String> identifiers =
            Arrays.asList((String[]) doc.getPropertyValue("hrv:identifier"));
        Optional<String> remoteUrl = identifiers.stream().filter(
            s -> s.startsWith("http:")).findFirst();
        if (remoteUrl.isPresent()) {
            URI uri = null;
            try {
                uri = new URI(remoteUrl.get());
            } catch (URISyntaxException e) {
                LOG.error("Error parsing uri", e);
                doc.setPropertyValue(STATUS_PROP,
                                     String.format("failed: %s", e.getMessage()));
                session.saveDocument(doc);
                return doc;
            }
            LOG.info("Attempting to retrieve {}", uri);
            try {
                retrieve(doc, uri);
            } catch (IOException | URISyntaxException e) {
                LOG.error("Error retrieving uri", e);
                doc.setPropertyValue(STATUS_PROP,
                                     String.format("failed: %s", e.getMessage()));
                session.saveDocument(doc);
                return doc;
            }
        }
        return doc;
    }

    // FIXME: this assumes it's HathiTrust
    // TODO: retrieval-strategy-per-repo or adaptive strategies or both
    // TODO: run retrieval asynchronously
    private void retrieve(DocumentModel doc, URI uri) throws
        IOException,
        URISyntaxException {
        HttpContext httpContext = new HttpCoreContext();
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet getRequest = new HttpGet(uri);
        HttpResponse resp = client.execute(getRequest, httpContext);
        if (resp.getStatusLine().getStatusCode() != 200) {
            doc.setPropertyValue(STATUS_PROP,
                                 String.format("failed: %s", resp.getStatusLine().getReasonPhrase()));
            session.saveDocument(doc);
            return;
        }
        URI finalUri = getRequest.getURI();
        RedirectLocations locations = (RedirectLocations)
            httpContext.getAttribute(HttpClientContext.REDIRECT_LOCATIONS);
        if (locations != null) {
            finalUri = locations.get(locations.size() - 1);
        }
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme(finalUri.getScheme());
        uriBuilder.setHost(finalUri.getHost());
        uriBuilder.setPort(finalUri.getPort());
        URI baseUri = uriBuilder.build();
        HttpEntity entity = resp.getEntity();
        Header contentTypeHeader = resp.getFirstHeader("Content-Type");
        String charset = "iso-8859-1";
        if (contentTypeHeader != null) {
            NameValuePair headerCharset = contentTypeHeader.getElements()[0]
                .getParameterByName("charset");
            if (headerCharset != null) {
                charset = headerCharset.getValue();
            }
        }
        Document htmlDoc = Jsoup.parse(
            entity.getContent(), charset, baseUri.toString());
        EntityUtils.consumeQuietly(entity);
        Element blobLink = htmlDoc.select("a[id=fullPdfLink][rel=allow]").first();
        if (blobLink != null) {
            retrieveContent(doc, client, new URI(blobLink.attr("abs:href")));
        }

    }

    private void retrieveContent(DocumentModel doc,
                                 HttpClient client,
                                 URI uri) throws
        URISyntaxException,
        IOException {

        LOG.info("Attempting to retrieve {}", uri);
        HttpGet fileRequest = new HttpGet(uri);
        HttpResponse fileResponse = client.execute(fileRequest);
        if (fileResponse.getStatusLine().getStatusCode() == 200) {
            HttpEntity fileEntity = fileResponse.getEntity();
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            FileBlob blob = new FileBlob(fileEntity.getContent());
            blob.setMimeType(fileEntity.getContentType().getValue());
            blob.setFilename("retrievedFile");
            bh.setBlob(blob);
            doc.setPropertyValue(STATUS_PROP, "success");
            session.saveDocument(doc);
        } else {
            String msg = fileResponse.getStatusLine().toString();
            LOG.error("failed to retrieve {}: {}", uri, msg);
            doc.setPropertyValue(STATUS_PROP, String.format("failed: %s", msg));
            session.saveDocument(doc);
        }
    }
}
