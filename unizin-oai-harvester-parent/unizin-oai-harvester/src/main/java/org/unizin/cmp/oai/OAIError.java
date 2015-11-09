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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((errorCode == null) ? 0 : errorCode.hashCode());
        result = prime * result + ((errorCodeString == null) ? 0 : errorCodeString.hashCode());
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OAIError other = (OAIError) obj;
        if (errorCode != other.errorCode)
            return false;
        if (errorCodeString == null) {
            if (other.errorCodeString != null)
                return false;
        } else if (!errorCodeString.equals(other.errorCodeString))
            return false;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        return true;
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
