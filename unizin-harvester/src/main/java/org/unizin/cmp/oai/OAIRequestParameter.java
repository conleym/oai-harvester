package org.unizin.cmp.oai;


/**
 * Enumeration of all valid OAI-PMH request parameters, regardless of verb.
 *
 */
public enum OAIRequestParameter {
	FROM("from"),
	IDENTIFIER("identifier"),
	METADATA_PREFIX("metadataPrefix"),
	RESUMPTION_TOKEN("resumptionToken"),
	SET("set"),
	UNTIL("until");

	
	private final String param;
	private OAIRequestParameter(final String param) {
		this.param = param;
	}
	
	public String paramName() {
		return this.param;
	}
}
