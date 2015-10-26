package org.unizin.cmp.retrieval;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Strategy for retrieving content from HathiTrust by scraping its HTML
 * resource description pages.
 */
public class HathiTrustHtmlRetriever extends BaseRetriever {

    /**
     * Retrieve the document content by using the {@code handle.net}
     * identifier on {@code document}.
     *
     * <p>The high-level strategy used is:
     * <ol>
     *     <li>find the first {@code hrv:identifier} on {@code document} that
     *     begins with "{@code http:}"
     *     <li>if found, retrieve its content which is expected to be HTML
     *     <li>if HTML is retrieved, parse it and look for an anchor element
     *     with the id {@code fullPdfLink} and {@code rel=allow}
     *     <li>if such a link is found, retrieve its content
     *     <li>return a {@link org.nuxeo.ecm.core.api.Blob} with the content
     * </ol>
     *
     * @param client the HTTP client instance to use
     * @param document a document with the {@code Harvested} facet
     * @throws RetrievalException if there is no public fulltext link found, or
     *                            if an I/O or parsing error occurs.
     */
    @Override
    public Blob retrieveFileContent(CloseableHttpClient client, DocumentModel document)
            throws RetrievalException {
        Optional<String> htmlUrl = findRemoteUrl(document);
        if (htmlUrl.isPresent()) {
            Optional<String> dataUrl = extractDataUrl(client, htmlUrl.get());
            if (dataUrl.isPresent()) {
                return retrieveContent(client, dataUrl.get());
            } else {
                throw new RetrievalException("no public fulltext link found");
            }
        } else {
            String msg = String.format("no remote URL found on document %s",
                                       document.getId());
            throw new RetrievalException(msg);
        }
    }

    private Optional<String> extractDataUrl(HttpClient client, String htmlUrl) {
        URI uri;

        try {
            uri = new URI(htmlUrl);
        } catch (URISyntaxException e) {
            throw new RetrievalException(e);
        }

        HttpContext httpCtx = new HttpCoreContext();
        HttpGet req = new HttpGet(uri);
        HttpResponse resp;

        try {
            resp = client.execute(req, httpCtx);
        } catch (IOException e) {
            throw new RetrievalException(e);
        }

        if (resp.getStatusLine().getStatusCode() != 200) {
            throw new RetrievalException(resp.getStatusLine().getReasonPhrase());
        }

        URI htmlBaseUri = unwindRedirectsIntoBaseUri(req, httpCtx);
        return parseHtml(resp, htmlBaseUri);
    }

    private Optional<String> parseHtml(HttpResponse resp, URI htmlBaseUri) {
        Optional<String> result = Optional.empty();
        HttpEntity entity = resp.getEntity();
        ContentType contentType = ContentType.getOrDefault(entity);
        try {
            Document htmlDoc = Jsoup.parse(entity.getContent(),
                                           contentType.getCharset().toString(),
                                           htmlBaseUri.toString());
            Element link = htmlDoc.select("a[id=fullPdfLink][rel=allow]").first();
            if (link != null) {
                String href = link.attr("abs:href");
                if (!href.isEmpty()) {
                    result = Optional.of(href);
                }
            }
            return result;
        } catch (IOException e) {
            throw new RetrievalException(e);
        } finally {
            EntityUtils.consumeQuietly(entity);
        }
    }

    private URI unwindRedirectsIntoBaseUri(HttpGet req, HttpContext httpCtx) {
        URI targetUri = req.getURI();
        RedirectLocations locations = (RedirectLocations)
                httpCtx.getAttribute(HttpClientContext.REDIRECT_LOCATIONS);
        if (locations != null && !locations.isEmpty()) {
            targetUri = locations.get(locations.size() - 1);
        }
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme(targetUri.getScheme());
        uriBuilder.setHost(targetUri.getHost());
        uriBuilder.setPort(targetUri.getPort());
        try {
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new RetrievalException(e);
        }
    }

    private Optional<String> findRemoteUrl(DocumentModel document) {
        List<String> identifiers = Arrays.asList(
                (String[])document.getPropertyValue(IDENTIFIER_PROP));
        return identifiers.stream()
                .filter(s-> s.startsWith("http:")).findFirst();
    }
}
