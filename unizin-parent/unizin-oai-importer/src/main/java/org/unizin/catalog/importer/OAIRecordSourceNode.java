package org.unizin.catalog.importer;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.joda.time.DateTime;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.AbstractBlobHolder;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


public final class OAIRecordSourceNode implements SourceNode {
	
	private static final String[] EMPTY = new String[]{};
	private static final String[] UNTITLED = new String[]{"Untitled"};
	
	private static final Logger LOGGER = 
			LoggerFactory.getLogger(OAIRecordSourceNode.class);

	private static final String XPATH_FORMAT = 
			"/oai:record/oai:metadata/oai_dc:dc/dc:%s/text()";
	
    private final Document document;
    private final XPath xpath;
	private final Blob blob;
	private final URI baseURI;
	private final Calendar lastModified;
	private final Map<String, Serializable> properties;

	
	public OAIRecordSourceNode(final byte[] bytes, final Document document, 
			final URI baseURI)
			throws XMLStreamException, XPathExpressionException {
		this.document = document;
		this.xpath = XPathFactory.newInstance().newXPath();
		this.xpath.setNamespaceContext(OAIConstants.OAI_NS_CONTEXT);
		this.blob = Blobs.createBlob(bytes);
		this.baseURI = baseURI;
		this.lastModified = parseLastModified();
		this.properties = parseProperties();	
	}
	
	
	private Calendar parseLastModified() throws XPathExpressionException {
		final String datestamp = (String) xpath.evaluate(
				"/oai:record/oai:header/oai:datestamp/text()",
				document,
				XPathConstants.STRING);
		final DateTime dt = new DateTime(datestamp);
		return dt.toCalendar(null); // Default locale.
	}
	

	private Map<String, Serializable> parseProperties() 
		throws XPathExpressionException {
		final Map<String, Serializable> result = new HashMap<>();
		String[] titles = listXPath("title");
		if (titles.length == 0) {
			titles = UNTITLED;
		}
		result.put("dc:title", titles[0]);
		// TODO: other dc: fields?
		result.put("hrv:title", listXPath("title"));
        result.put("hrv:creator", listXPath("creator"));
        result.put("hrv:subject", listXPath("subject"));
        result.put("hrv:description", listXPath("description"));
        result.put("hrv:publisher", listXPath("publisher"));
        result.put("hrv:contributor", listXPath("contributor"));
        result.put("hrv:date", listXPath("date"));
        result.put("hrv:type", listXPath("type"));
        result.put("hrv:format", listXPath("format"));
        result.put("hrv:identifier", listXPath("identifier"));
        result.put("hrv:source", listXPath("source"));
        result.put("hrv:language", listXPath("language"));
        result.put("hrv:relation", listXPath("relation"));
        result.put("hrv:coverage", listXPath("coverage"));
        result.put("hrv:rights", listXPath("rights"));
		result.put("hrv:sourceRepository", String.valueOf(baseURI));
		
		String identifier = (String) xpath.evaluate(
				"/oai:record/oai:header/oai:identifier/text()",
				document,
				XPathConstants.STRING);
		if (identifier == null || "".equals(identifier)) {
			identifier = "NONE";
		}
		result.put("hrv:oaiIdentifier", identifier);
		return result;
	}
	

	private String[] listXPath(final String expression)
			throws XPathExpressionException {
		final String dcExpression = String.format(XPATH_FORMAT, expression);
		final Object result = xpath.evaluate(dcExpression, document, 
				XPathConstants.NODESET);
		if (result instanceof NodeList) {
			return toList((NodeList)result).toArray(EMPTY);
		}
		return EMPTY;
	}
	
	
	private List<String> toList(final NodeList nodes) {
		final ArrayList<String> result = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			final Object o = nodes.item(i);
			if (o instanceof Text) {
				result.add(((Text)o).getNodeValue());
			} else {
				LOGGER.warn("Node of type {} found. Expected Text.",
						o.getClass().getName());
			}
		}
		return result;
	}
	
	@Override
	public boolean isFolderish() {
		return false;
	}

	@Override
	public BlobHolder getBlobHolder() throws IOException {
		return new AbstractBlobHolder() {
			@Override
			public Serializable getProperty(String name) {
				return properties.get(name);
			}

			@Override
			public Map<String, Serializable> getProperties() {
				return properties;
			}

			@Override
			public Blob getBlob() throws ClientException {
				return blob;
			}

			@Override
			protected String getBasePath() {
				return "";
			}

			@Override
			public Calendar getModificationDate() throws ClientException {
				return lastModified;
			}
		};
	}

	@Override
	public List<SourceNode> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String getName() {
		final Serializable title = properties.get("dc:title");
		return title == null ? UNTITLED[0] : title.toString();
	}

	@Override
	public String getSourcePath() {
		return "";
	}
}
