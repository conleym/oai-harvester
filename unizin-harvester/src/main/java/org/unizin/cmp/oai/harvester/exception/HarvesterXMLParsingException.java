package org.unizin.cmp.oai.harvester.exception;


/**
 * Thrown when the harvester encounters an error parsing XML in a repository's
 * response.
 *
 */
public class HarvesterXMLParsingException extends HarvesterException {
	private static final long serialVersionUID = 1L;

	public HarvesterXMLParsingException() {
		super();
	}

	public HarvesterXMLParsingException(final String message,
			final Throwable cause) {
		super(message, cause);
	}

	public HarvesterXMLParsingException(final String message) {
		super(message);
	}

	public HarvesterXMLParsingException(final Throwable cause) {
		super(cause);
	}
}
