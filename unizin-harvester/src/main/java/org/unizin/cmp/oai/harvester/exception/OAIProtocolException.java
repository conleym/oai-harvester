package org.unizin.cmp.oai.harvester.exception;

import java.util.Collections;
import java.util.List;

import org.unizin.cmp.oai.OAIError;

/**
 * Exception thrown when a repository sends back an <a
 * href="http://www.openarchives.org/OAI/openarchivesprotocol.html#ErrorConditions">
 * OAI-PMH error code</a> in its response.
 *
 */
public final class OAIProtocolException extends HarvesterException {
    private static final long serialVersionUID = 1L;

    private final List<OAIError> errors;

    /**
    * Create a new instance with a list of OAI protocol errors.
    *
    * @param errors a list of {@link OAIError}s that occurred. The list itself
    * should implement {@link java.io.Serializable} if the instance is to be
    * serialized.
    */
    public OAIProtocolException(final List<OAIError> errors) {
        super("OAI protocol error(s).");
        // Yes, unmodifiableList returns a Serializable List.
        this.errors = Collections.unmodifiableList(errors);
    }

    public List<OAIError> getOAIErrors() {
        return Collections.unmodifiableList(errors);
    }
}
