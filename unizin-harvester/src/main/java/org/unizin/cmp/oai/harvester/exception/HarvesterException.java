package org.unizin.cmp.oai.harvester.exception;


/**
 * Instances of this exception or one of its subclasses are thrown by the
 * harvester.
 *
 */
public class HarvesterException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public HarvesterException() {
		super();
	}

	public HarvesterException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public HarvesterException(final String message) {
		super(message);
	}

	public HarvesterException(final Throwable cause) {
		super(cause);
	}	
}
