package org.unizin.catalog.importer.oai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.catalog.OAIRecordContext;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Record {
    private static final Logger LOGGER = LoggerFactory.getLogger(Record.class);
    private final Document document;
    private final XPath xPath;
    private final URI baseUri;


    public Record(Document xmlDoc, URI baseUri) {
        this.document = xmlDoc;
        this.baseUri = baseUri;
        xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(new OAIRecordContext());

    }

    public Optional<String> getTitle() {
        return getAsOptional("/oai:record/oai:metadata/oai_dc:dc/dc:title[1]");
    }

    public Optional<String> getOaiIdentifier() {
        return getAsOptional("/oai:record/oai:header/oai:identifier");
    }

    private Optional<String> getAsOptional(String xPathStr) {
        String result = null;
        try {
            result = (String) xPath.evaluate(
                    xPathStr, document, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            LOGGER.debug("no {} found", xPathStr);
        }
        return Optional.ofNullable(result);
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public List<String> getDCElements(String dcLocalName) {
        String xPathStr =
                String.format("/oai:record/oai:metadata/oai_dc:dc/dc:%s/text()",
                              dcLocalName);
        List<String> results = new ArrayList<>();
        try {
            NodeList nodes = (NodeList) xPath.evaluate(
                    xPathStr, document, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++){
                String val = nodes.item(i).getNodeValue();
                results.add(val);
            }
        } catch (XPathExpressionException e) {
            LOGGER.debug("no {} found", xPathStr);
        }
        return results;
    }

    public String getRawContent() {
        DOMImplementationLS domLs = (DOMImplementationLS) document.getImplementation();
        LSSerializer serializer = domLs.createLSSerializer();
        return serializer.writeToString(document);
    }
}
