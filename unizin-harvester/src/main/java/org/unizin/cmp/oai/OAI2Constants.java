package org.unizin.cmp.oai;

import javax.xml.namespace.QName;


/**
 * OAI-PMH constants.
 * <p>
 * Most of these are XML-related.
 */
public final class OAI2Constants {
	
	private static QName oai2QName(final String localPart) {
		return new QName(OAI_2_NS_URI, localPart);
	}
	
	private static QName attrQName(final String name) {
		return new QName("", name);
	}
	
	/**
	 * OAI 2.0 namespace URI.
	 */
	public static final String OAI_2_NS_URI = 
			"http://www.openarchives.org/OAI/2.0/";
	
	/**
	 * OAI_DC namespace URI.
	 * <p>
	 * Elements from this namespace are used in the default metadata format,
	 * which all repositories are required to support.
	 * @see #DEFAULT_METADATA_PREFIX
	 */
	public static final String OAI_DC_NS_URI = 
			"http://www.openarchives.org/OAI/2.0/oai_dc/";
	
	/**
	 * DC elements namespace URI.
	 * <p>
	 * Elements from this namespace are used in the default metadata format,
	 * which all repositories are required to support.
	 * @see #DEFAULT_METADATA_PREFIX
	 */
	public static final String DC_NS_URI = "http://purl.org/dc/elements/1.1/";
	
	/**
	 * Default <a href=
	 * "http://www.openarchives.org/OAI/openarchivesprotocol.html#metadataPrefix">
	 * metadataPrefix</a> for harvests if none is explicitly given.
	 * <p>
	 * All repositories are required to support this prefix and its
	 * corresponding format.
	 * </p>
	 */
	public static final String DEFAULT_METADATA_PREFIX = "oai_dc";
	
	/**
	 * Value of the {@code status} attribute on &lt;header&gt; used to indicate
	 * that a record has been deleted.
	 */
	public static final String DELETED_STATUS = "deleted";
	
	/** Name of the parameter containing the verb. */
	public static final String VERB_PARAM_NAME = "verb";
	
	// QNames of tags for each OAI-PMH request verb.
	public static final QName GET_RECORD = oai2QName("GetRecord");
	public static final QName IDENTIFY = oai2QName("Identify");
	public static final QName LIST_IDENTIFIERS = oai2QName("ListIdentifiers");
	public static final QName LIST_METADATA_FORMATS = 
			oai2QName("ListMetadataFormats");
	public static final QName LIST_RECORDS = oai2QName("ListRecords");
	public static final QName LIST_SETS = oai2QName("ListSets");

	// QNames of other OAI-PMH tags.
	public static final QName DATESTAMP = oai2QName("datestamp");
	public static final QName ERROR = oai2QName("error");
	public static final QName HEADER = oai2QName("header");
	public static final QName IDENTIFIER = oai2QName("identifier");
	public static final QName OAI_PMH = oai2QName("OAI-PMH");
	public static final QName RECORD = oai2QName("record");
	public static final QName RESPONSE_DATE = oai2QName("responseDate");
	public static final QName REQUEST = oai2QName("request");
	public static final QName RESUMPTION_TOKEN = oai2QName("resumptionToken");
	
	// QNames of attributes (these have no namespace).
	/** Status code on &lt;error&gt;. */
	public static final QName ERROR_CODE_ATTR = attrQName("code");
	/** Optional status (deleted or not) on &lt;header&gt;. */
	public static final QName HEADER_STATUS_ATTR = attrQName("status");
	/** Optional cursor attribute on &lt;resumptionToken&gt;. */
	public static final QName RT_CURSOR_ATTR = attrQName("cursor");
	/** Optional completeListSize attribute on &lt;resumptionToken&gt;. */
	public static final QName RT_COMPLETE_LIST_SIZE_ATTR = 
			attrQName("completeListSize");
	/** Optional expirationDate attribute on &lt;resumptionToken&gt;. */
	public static final QName RT_EXPIRATION_DATE_ATTR = 
			attrQName("expirationDate");
	
	/** No instances allowed. */
	private OAI2Constants() {}
}
