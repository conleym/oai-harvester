package org.unizin.catalog.importer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.io.ByteStreams;


final class ListRecordsResponseSplitter
	implements Iterable<OAIRecordSourceNode> {
	
	private final DocumentBuilderFactory docBuilderFactory =
			XMLStreams.docBuilderFactory();
	private final XMLInputFactory inputFactory = XMLStreams.inputFactory();
	private final XMLOutputFactory outputFactory = XMLStreams.outputFactory();
	private final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
	private final XMLEventReader eventReader;
	private boolean hasNext = false;
	private URI baseURL;

	
	public ListRecordsResponseSplitter(final InputStream in)
			throws XMLStreamException, URISyntaxException {
		this.eventReader = inputFactory.createXMLEventReader(in);
		while (this.eventReader.hasNext()) {
			XMLEvent event = this.eventReader.peek();
			if (isRecordStart(event)) {
				break;
			}
			event = this.eventReader.nextEvent();
			if (isRequestStart(event)) {
				// TODO: Double check this.
				if (this.eventReader.hasNext()) {
					event = this.eventReader.peek();
					if (event.isCharacters()) {
						this.baseURL = new URI(event.asCharacters().getData());
						break;
					}
				}
			}
		}
		skipToNextRecordStart();
	}

	private static boolean isRequestStart(final XMLEvent event) {
		return event.isStartElement() &&
				OAIConstants.REQUEST.equals(event.asStartElement().getName());
	}
	
	private static boolean isRecordStart(final XMLEvent event) {
		return event.isStartElement() && 
				OAIConstants.RECORD.equals(event.asStartElement().getName());
	}

	
	private static boolean isRecordEnd(final XMLEvent event) {
		return event.isEndElement() &&
				OAIConstants.RECORD.equals(event.asEndElement().getName());
	}
	
	
	private OAIRecordSourceNode recordOf(final byte[] bytes)
			throws XMLStreamException, IOException {
		try {
			final Document doc = docBuilderFactory.newDocumentBuilder()
					.parse(new ByteArrayInputStream(bytes));
			ByteStreams.copy(new ByteArrayInputStream(bytes), System.out);
			return new OAIRecordSourceNode(bytes, doc, baseURL);
		} catch (final ParserConfigurationException | XPathExpressionException |
				SAXException e) {
			// TODO: better exception.
			throw new RuntimeException(e);
		}
	}
	
	
	private OAIRecordSourceNode nextRecord()
			throws XMLStreamException, IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final XMLEventWriter writer = outputFactory.createXMLEventWriter(baos);
		if (hasNext) {
			final XMLEvent event = this.eventReader.nextEvent();
			writer.add(event);
            writer.add(
                    eventFactory.createNamespace(OAIConstants.OAI_NS_URI));
		}
		while (eventReader.hasNext()) {
			final XMLEvent event = this.eventReader.nextEvent();
			writer.add(event);
			if (isRecordEnd(event)) {
				skipToNextRecordStart();
				return recordOf(baos.toByteArray());
			}
		}
		throw new XMLStreamException("Unexpected end of input looking for end of record.");
	}
	
	
	private void skipToNextRecordStart() throws XMLStreamException {
		try {
			while (this.eventReader.hasNext()) {
				final XMLEvent event = this.eventReader.peek();
				if (isRecordStart(event)) {
					this.hasNext = true;
					return;
				}
				this.eventReader.nextEvent();
			}
			// Got to end of input w/o finding a new record => no more records.
			this.hasNext = false;
		} catch (final XMLStreamException e) {
			// Error reading => we're not getting any more records.
			this.hasNext = false;
			throw e;
		}
	}

	
	@Override
	public Iterator<OAIRecordSourceNode> iterator() {
		return new IteratorImpl();
	}
	
	
	private class IteratorImpl implements Iterator<OAIRecordSourceNode> {
		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public OAIRecordSourceNode next() {
			try {
				return nextRecord();
			} catch (final IOException e) {
				throw new IORuntimeException(e);
			} catch (final XMLStreamException e) {
				throw new XMLStreamRuntimeException(e);
			}
		}
	}
}
