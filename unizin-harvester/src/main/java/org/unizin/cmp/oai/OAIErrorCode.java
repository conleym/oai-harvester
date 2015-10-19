package org.unizin.cmp.oai;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of standard <a href=
 * "http://www.openarchives.org/OAI/openarchivesprotocol.html#ErrorConditions">
 * OAI-PMH error codes</a>.
 *
 */
public enum OAIErrorCode {
	BAD_ARGUMENT("badArgument"),
	BAD_RESUMPTION_TOKEN("badResumptionToken"),
	BAD_VERB("badVerb"),
	CANNOT_DISSEMINATE_FORMAT("cannotDisseminateFormat"),
	ID_DOES_NOT_EXIST("idDoesNotExist"),
	NO_RECORDS_MATCH("noRecordsMatch"),
	NO_METADATA_FORMATS("noMetadataFormats"),
	NO_SET_HIERARCHY("noSetHierarchy");

	private final String code;
	private OAIErrorCode(final String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

	private static final Map<String, OAIErrorCode> CODE_TO_ENUM;
	static {
		final OAIErrorCode[] values = OAIErrorCode.values();
		final Map<String, OAIErrorCode> m = new HashMap<>(values.length);
		for (final OAIErrorCode value : values) {
			m.put(value.code(), value);
		}
		CODE_TO_ENUM = Collections.unmodifiableMap(m);
	}
	
	public static OAIErrorCode valueOfCode(final String codeString) {
		return CODE_TO_ENUM.get(codeString);
	}
}
