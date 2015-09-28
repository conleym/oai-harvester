package org.unizin.catalog.importer;

/** 
 * Runtime exception wrapper for checked exceptions thrown at inconvenient
 * times and places.
 */
public final class ImporterException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	ImporterException(final Throwable cause) {
		super(cause);
	}
}
