package org.unizin.catalog.importer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

final class XML {
	static XMLInputFactory inputFactory() {
		final XMLInputFactory factory = XMLInputFactory.newFactory();
		// Force coalescing of adjacent character events into a single event.
		factory.setProperty("javax.xml.stream.isCoalescing", true);
		return factory;
	}
	
	static XMLOutputFactory outputFactory() {
		return XMLOutputFactory.newFactory();
	}
	
	static DocumentBuilderFactory docBuilderFactory() {
		final DocumentBuilderFactory factory = 
				DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		return factory;
	}


	/** No instances allowed. */
	private XML() {}
}
