package org.unizin.cmp.importer;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.joda.time.DateTime;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.AbstractBlobHolder;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
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

	private static final String DC_XPATH_FORMAT = 
			"/oai:record/oai:metadata/oai_dc:dc/dc:%s/text()";

	private final Calendar lastModified;
	private final Map<String, Serializable> properties;

	public OAIRecordSourceNode(final byte[] bytes, final Document document, 
			final URI baseURI) throws XMLStreamException, IOException {
		final XPath xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(OAIConstants.OAI_NS_CONTEXT);
		this.lastModified = parseLastModified(xpath, document);
		this.properties = parseProperties(xpath, document);
		
		this.properties.put("hrv:sourceRepository", String.valueOf(baseURI));
		this.properties.put("hrv:rawMetadataContent", new ByteArrayBlob(bytes));	
	}


	private Calendar parseLastModified(final XPath xpath,
			final Document document) {
		final String datestamp = stringXPath(
				"/oai:record/oai:header/oai:datestamp/text()",
				xpath,
				document);
		final DateTime dt = new DateTime(datestamp);
		return dt.toCalendar(null); // Default locale.
	}


	private Map<String, Serializable> parseProperties(final XPath xpath,
			final Document document) {
		final Map<String, Serializable> result = new HashMap<>();

		final Function<String, String[]> dcList = dcElement -> 
			dcListXPath(dcElement, xpath, document);

		String[] titles = dcList.apply("title");
		if (titles.length == 0) {
			titles = UNTITLED;
		}

		result.put("dc:title", titles[0]);
		// TODO: other dc: fields?
		result.put("hrv:title", dcList.apply("title"));
		result.put("hrv:creator", dcList.apply("creator"));
		result.put("hrv:subject", dcList.apply("subject"));
		result.put("hrv:description", dcList.apply("description"));
		result.put("hrv:publisher", dcList.apply("publisher"));
		result.put("hrv:contributor", dcList.apply("contributor"));
		result.put("hrv:date", dcList.apply("date"));
		result.put("hrv:type", dcList.apply("type"));
		result.put("hrv:format", dcList.apply("format"));
		result.put("hrv:identifier", dcList.apply("identifier"));
		result.put("hrv:source", dcList.apply("source"));
		result.put("hrv:language", dcList.apply("language"));
		result.put("hrv:relation", dcList.apply("relation"));
		result.put("hrv:coverage", dcList.apply("coverage"));
		result.put("hrv:rights", dcList.apply("rights"));

		String identifier = stringXPath(
				"/oai:record/oai:header/oai:identifier/text()",
				xpath,
				document);
		if (identifier == null || "".equals(identifier)) {
			identifier = "NONE";
		}
		result.put("hrv:oaiIdentifier", identifier);
		return result;
	}
	
	
	private String stringXPath(final String expression, final XPath xpath,
			final Document document) {
		try {
			return (String)xpath.evaluate(expression, document,
					XPathConstants.STRING);
		} catch (final XPathExpressionException e) {
			throw new ImporterException(e);
		}
		
	}


	private String[] dcListXPath(final String expression, final XPath xpath,
			final Document document) {
		try {
			final String dcExpression = String.format(DC_XPATH_FORMAT,
					expression);
			final Object result = xpath.evaluate(dcExpression, document, 
					XPathConstants.NODESET);
			if (result instanceof NodeList) {
				// Nuxeo's going to turn lists into arrays anyhow, and String[]
				// is Serializable, as opposed to List<String>, which isn't 
				// (any implementation we would use is, of course, but 
				// the compiler will still complain).
				return toList((NodeList)result).toArray(EMPTY);
			}
		} catch (final XPathExpressionException e) {
			throw new ImporterException(e);
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
			public Blob getBlob() {
				return null;
			}

			@Override
			protected String getBasePath() {
				return "";
			}

			@Override
			public Calendar getModificationDate() {
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
