package org.unizin.catalog.importer;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

public class OAIConstants {
	private OAIConstants() {}
	public static final String OAI_NS_URI = "http://www.openarchives.org/OAI/2.0/";
	public static final String DC_NS_URI = "http://purl.org/dc/elements/1.1/";
	public static final String OAI_DC_NS_URI = "http://www.openarchives.org/OAI/2.0/oai_dc/";
	public static final QName RESUMPTION_TOKEN = new QName(OAI_NS_URI, "resumptionToken");
	public static final QName CURSOR = new QName("cursor");
	public static final QName COMPLETE_LIST_SIZE = new QName("completeListSize");
	public static final QName EXPIRATION_DATE = new QName("expirationDate");
	public static final QName RECORD = new QName(OAI_NS_URI, "record");
	public static final QName REQUEST = new QName(OAI_NS_URI, "request");

	public static final NamespaceContext OAI_NS_CONTEXT = new OAIRecordContext();

	public static class OAIRecordContext implements NamespaceContext {

		@Override
		public String getNamespaceURI(String prefix) {
			String result;
			switch (prefix) {
			case "oai":
				result = OAI_NS_URI;
				break;
			case "dc":
				result = DC_NS_URI;
				break;
			case "oai_dc":
				result = OAI_DC_NS_URI;
				break;
			default:
				result = null;
			}
			return result;
		}

		@Override
		public String getPrefix(String namespaceURI) {
			return null;
		}

		@Override
		public Iterator<?> getPrefixes(String namespaceURI) {
			return null;
		}
	}
}
