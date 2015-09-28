package org.unizin.catalog.importer;

import java.io.IOException;

public class IORuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public IORuntimeException(final IOException e) {
		super(e);
	}
}
