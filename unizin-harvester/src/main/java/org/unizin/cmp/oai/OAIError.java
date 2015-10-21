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
		this.message = (message == null) ? "" : message;
	}
	
	/**
	 * Get the raw string representation of the error code associated with this
	 * error.
	 * <p>
	 * This exists to allow support for any nonstandard error codes that might
	 * be received from repositories.
	 * 
	 * @return the raw string representation of the error code associated with
	 *         this error.
	 */
	public String getErrorCodeString() {
		return errorCodeString;
	}
	
	/**
	 * Get this error's code.
	 * 
	 * @return an optional containing this error's code, or an empty optional if
	 *         the code is nonstandard or was not provided.
	 */
	public Optional<OAIErrorCode> getErrorCode() {
		// Optional is not Serializable, so we can't store it.
		return Optional.ofNullable(errorCode);
	}
	
	/**
	 * Get the message associated with this error.
	 * <p>
	 * This method never returns {@code null}. If no message was provided,
	 * returns the empty string.
	 * 
	 * @return the non-{@code null} message sent with this error.
	 */
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
