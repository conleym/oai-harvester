package org.unizin.catalog.importer;

import java.io.IOException;
import java.io.InterruptedIOException;


/**
 * Runtime exception wrapper for {@code IOException}s thrown at inconvenient
 * times and places.
 * <p>
 * Separate from {@link ImporterException}, because some {@code IOException}s
 * can indicate that a thread was interrupted (see 
 * {@link InterruptedIOException}). We can make sure we don't mask
 * those by always catching {@code IORuntimeException} and throwing its 
 * {@code cause}.
 *
 */
public class IORuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public IORuntimeException(final IOException e) {
		super(e);
	}

	@Override
	public IOException getCause() {
		return (IOException)super.getCause();
	}
}
