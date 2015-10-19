package org.unizin.cmp.oai;

import java.io.Serializable;
import java.util.Optional;


/**
 * Container for information about <a href=
 * "http://www.openarchives.org/OAI/openarchivesprotocol.html#ErrorConditions">
 * OAI error conditions</a>.
 *
 */
public final class OAIError implements Serializable {
	// Serializable because exceptions containing this will be also.
	// We're never going to serialize exceptions, but we shouldn't prevent
	// others from doing so.
	private static final long serialVersionUID = 1L;
	
	private final String errorCodeString;
	private final OAIErrorCode errorCode;
	private final String message;
	
	public OAIError(final String errorCodeString) {
		this(errorCodeString, "");
	}
	
	public OAIError(final String errorCodeString, final String message) {
		this.errorCodeString = errorCodeString;
		this.errorCode = OAIErrorCode.valueOfCode(errorCodeString);
		this.message = message;
	}
	
	public String getErrorCodeString() {
		return errorCodeString;
	}
	
	public Optional<OAIErrorCode> getErrorCode() {
		// Optional is not Serializable, so we can't store it.
		return Optional.ofNullable(errorCode);
	}
	
	public String getMessage() { 
		return message; 
	}
	
	@Override
	public String toString() {
		return new StringBuilder(this.getClass().getName())
				.append("[")
				.append("errorCode=")
				.append(errorCodeString)
				.append(", message=")
				.append(message)
				.append("]")
				.toString();
	}
}
