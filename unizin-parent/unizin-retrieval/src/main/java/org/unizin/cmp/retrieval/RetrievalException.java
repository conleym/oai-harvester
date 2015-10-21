package org.unizin.cmp.retrieval;

import org.nuxeo.ecm.core.api.NuxeoException;

public class RetrievalException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public RetrievalException() {
        super();
    }

    public RetrievalException(String message) {
        super(message);
    }

    public RetrievalException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetrievalException(Throwable cause) {
        super(cause);
    }
}
