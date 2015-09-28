package org.unizin.catalog.importer;

import javax.xml.stream.XMLStreamException;

public class XMLStreamRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public XMLStreamRuntimeException(final XMLStreamException e) {
		super(e);
	}
}
