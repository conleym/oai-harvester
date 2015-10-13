package org.unizin.cmp.search.operations;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.DocumentModel;
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

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
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
        URI finalUri = getRequest.getURI();
        RedirectLocations locations = (RedirectLocations)
                httpContext.getAttribute(HttpClientContext.REDIRECT_LOCATIONS);
        if (locations != null) {
            finalUri = locations.get(locations.size() - 1);
        }
        HttpEntity entity = resp.getEntity();
        HtmlDocumentBuilder parser = new HtmlDocumentBuilder();
        Document htmlDoc = parser.parse(entity.getContent());
        EntityUtils.consumeQuietly(entity);
        XPath xPath = XPathFactory.newInstance().newXPath();
        String pdfPath = xPath.evaluate(
                "//a[@id='fullPdfLink' and @rel='allow']/@href", htmlDoc);
        if (pdfPath != null) {
            URIBuilder uriBuilder = new URIBuilder();
            uriBuilder.setScheme(finalUri.getScheme());
            uriBuilder.setHost(finalUri.getHost());
            uriBuilder.setPath(pdfPath);
            URI fullPdfUri = uriBuilder.build();
            LOG.info("Attempting to retrieve {}", fullPdfUri);
            HttpHead headRequest = new HttpHead(fullPdfUri);
            HttpResponse headResponse = client.execute(headRequest);
            Header disposition = headResponse.getFirstHeader("Content-disposition");
            LOG.warn("{}", disposition.getElements()[0].getParameterByName("filename"));
        }

    }
}
