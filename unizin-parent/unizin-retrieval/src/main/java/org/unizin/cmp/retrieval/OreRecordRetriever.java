package org.unizin.cmp.retrieval;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Strategy for retrieving content from repositories that support the
 * "ore" metadata format.
 */
public class OreRecordRetriever extends BaseRetriever {
    /**
     * Retrieve the document content by using the {@code oaiIdentifier}
     * value on {@code document}.
     * <ol>
     *     <li>query the source repository with the identifier, using the "ore"
     *     metadata prefix
     *     <li>find the first atom:link element with length, type, and href elements
     *     <li>if found, dereference the href and treat the response as
     *     document content
     * </ol>
     *
     * @param httpClient the HTTP client instance to use
     * @param document a document with the {@code Harvested} facet
     * @throws RetrievalException if there is an error response from the
     * OAI repository, or if an I/O or parsing error occurs
     */
    @Override
    public Blob retrieveFileContent(CloseableHttpClient httpClient,
                                    DocumentModel document) throws
            RetrievalException {
        Document oreMetadata = retrieveOreMetadata(httpClient, document);
        Optional<String> bitstreamUrl = extractBitstreamUrl(oreMetadata);
        if (!bitstreamUrl.isPresent()) {
            throw new RetrievalException("no public fulltext link found");
        }
        return retrieveContent(httpClient, bitstreamUrl.get());
    }

    private Optional<String> extractBitstreamUrl(Document oreMetadata) {
        Optional<String> result = Optional.empty();
        XPath xp = XPathFactory.newInstance().newXPath();
        xp.setNamespaceContext(new OAIConstants.OAIRecordContext());
        try {
            String url = (String) xp.evaluate(
                    "//atom:link[@href and @length and @type][1]/@href",
                    oreMetadata, XPathConstants.STRING);
            if (url != null && url.length() > 0) {
                result = Optional.of(url);
            }
        } catch (XPathExpressionException e) {
            throw new RetrievalException(e);
        }
        return result;
    }

    private Document retrieveOreMetadata(CloseableHttpClient httpClient,
                                         DocumentModel document) {
        String oaiIdentifier =
                (String) document.getPropertyValue("hrv:oaiIdentifier");
        String baseUrl = (String) document.getPropertyValue("hrv:sourceRepository");
        // FIXME: use the OAI harvester library
        URI recordUri = buildOreRecordUri(baseUrl, oaiIdentifier);
        HttpGet get = new HttpGet(recordUri);

        try (CloseableHttpResponse resp = httpClient.execute(get)){
            if (resp.getStatusLine().getStatusCode() != 200) {
                throw new RetrievalException(String.format(
                        "unable to retrieve item metadata, %s",
                        resp.getStatusLine().getReasonPhrase()));
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder parser = dbf.newDocumentBuilder();
            return parser.parse(resp.getEntity().getContent());
        } catch (IOException e) {
            throw new RetrievalException(e);
        } catch (ParserConfigurationException e) {
            throw new RetrievalException(e);
        } catch (SAXException e) {
            throw new RetrievalException(e);
        }
    }

    private URI buildOreRecordUri(String baseUrl, String oaiIdentifier) {
        try {
            URIBuilder urib = new URIBuilder(baseUrl);
            urib.setParameter("identifier", oaiIdentifier);
            urib.setParameter("verb", "GetRecord");
            urib.setParameter("metadataPrefix", "ore");
            return urib.build();
        } catch (URISyntaxException e) {
            throw new RetrievalException(e);
        }
    }
}
