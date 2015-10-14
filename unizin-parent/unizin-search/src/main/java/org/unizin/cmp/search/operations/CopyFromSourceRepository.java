package org.unizin.cmp.search.operations;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.retrieve.HtmlDocumentBuilder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
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

    @Context
    CoreSession session;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        doc = session.getDocument(doc.getRef());
        if (!doc.hasFacet("Harvested")) {
            LOG.warn("{} called on document without Harvested facet", ID);
            return doc;
        }
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
                return doc;
            }
            LOG.info("Attempting to retrieve {}", uri);
            try {
                retrieve(doc, uri);
            } catch (IOException | SAXException | XPathException | URISyntaxException e) {
                LOG.error("Error retrieving uri", e);
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
            SAXException, XPathExpressionException, URISyntaxException {
        HttpContext httpContext = new HttpCoreContext();
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet getRequest = new HttpGet(uri);
        HttpResponse resp = client.execute(getRequest, httpContext);
        HttpEntity entity = resp.getEntity();
        HtmlDocumentBuilder parser = new HtmlDocumentBuilder();
        Document htmlDoc = parser.parse(entity.getContent());
        EntityUtils.consumeQuietly(entity);
        XPath xPath = XPathFactory.newInstance().newXPath();
        String blobPath = xPath.evaluate(
                "//a[@id='fullPdfLink' and @rel='allow']/@href", htmlDoc);
        URI finalUri = getRequest.getURI();
        RedirectLocations locations = (RedirectLocations)
                httpContext.getAttribute(HttpClientContext.REDIRECT_LOCATIONS);
        if (locations != null) {
            finalUri = locations.get(locations.size() - 1);
        }
        if (blobPath != null) {
            retrieveContent(doc, client, new URI(blobPath), finalUri);
        }

    }

    private void retrieveContent(DocumentModel doc, HttpClient client,
                                 URI blobPath, URI uri) throws
            URISyntaxException,
            IOException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme(uri.getScheme());
        uriBuilder.setHost(uri.getHost());
        uriBuilder.setPort(uri.getPort());
        uriBuilder.setPath(blobPath.getPath());
        uriBuilder.setCustomQuery(blobPath.getQuery());
        URI fullPdfUri = uriBuilder.build();
        LOG.info("Attempting to retrieve {}", fullPdfUri);
        HttpGet fileRequest = new HttpGet(fullPdfUri);
        HttpResponse fileResponse = client.execute(fileRequest);
        if (fileResponse.getStatusLine().getStatusCode() == 200) {
            HttpEntity fileEntity = fileResponse.getEntity();
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            FileBlob blob = new FileBlob(fileEntity.getContent());
            blob.setMimeType(fileEntity.getContentType().getValue());
            blob.setFilename("retrievedFile");
            bh.setBlob(blob);
            session.saveDocument(doc);
        } else {
            LOG.warn("failed to retrieve {}: {}", fullPdfUri,
                     fileResponse.getStatusLine());
        }
    }
}
