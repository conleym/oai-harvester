package org.unizin.cmp.oai.harvester;

import static org.unizin.cmp.oai.OAI2Constants.ERROR;
import static org.unizin.cmp.oai.OAI2Constants.ERROR_CODE_ATTR;
import static org.unizin.cmp.oai.OAI2Constants.RESPONSE_DATE;
import static org.unizin.cmp.oai.OAI2Constants.RESUMPTION_TOKEN;
import static org.unizin.cmp.oai.OAI2Constants.RT_COMPLETE_LIST_SIZE_ATTR;
import static org.unizin.cmp.oai.OAI2Constants.RT_CURSOR_ATTR;
import static org.unizin.cmp.oai.OAI2Constants.RT_EXPIRATION_DATE_ATTR;

import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.unizin.cmp.oai.OAIError;
import org.unizin.cmp.oai.OAIXMLUtils;
import org.unizin.cmp.oai.ResumptionToken;
import org.unizin.cmp.oai.harvester.exception.HarvesterXMLParsingException;
import org.unizin.cmp.oai.harvester.exception.OAIProtocolException;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;


/**
 * Internal-use-only class used to parse OAI-PMH responses on behalf of the
 * harvester.
 * <p>
 * Instances can be reused for multiple harvests.
 *
 */
final class OAIResponseParser {
	private final XMLInputFactory inputFactory;
	private final Logger logger;

	OAIResponseParser(final XMLInputFactory inputFactory, final Logger logger) {
		this.inputFactory = inputFactory;
		this.logger = logger;
	}

	/**
	 * Parse a response from an OAI repository and update the harvest state
	 * appropriately.
	 * 
	 * @param in
	 *            stream of the response content.
	 * @param harvest
	 *            the current harvest state.
	 * @param eventHandler
	 *            the event handler to which all {@code XMLEvents} will be sent.
	 * @throws XMLStreamException
	 *             if there's an error creating an {@link XMLEventReader} from
	 *             this instance's input factory's
	 *             {@link XMLInputFactory#createXMLEventReader(InputStream)}
	 *             method.
	 */
	void parse(final InputStream in, final Harvest harvest,
			final OAIEventHandler eventHandler) throws XMLStreamException {
		final XMLEventReader reader = inputFactory.createXMLEventReader(in);
		final List<OAIError> errorList = new ArrayList<>();
		/*
		 * Here, we need some gymnastics to ensure that protocol exceptions have
		 * "priority", i.e., that other exceptions encountered are suppressed in
		 * favor of protocol exceptions.
		 * 
		 * If there were protocol errors, any exception thrown in the try block
		 * will be added to the protocol exception as a suppressed exception.
		 * Otherwise, the exception thrown from the try block, if any, will be
		 * thrown.
		 * 
		 * This is basically the opposite of the priority handling used to
		 * suppress exceptions thrown in finally blocks in the harvester proper.
		 */
		RuntimeException tryException = null;
		try {
			final ResumptionToken resumptionToken = readEvents(reader,
					errorList, harvest, eventHandler);
			logger.debug("Got resumption token {}", resumptionToken);
			if (resumptionToken == null) {
				/*
				 * Cannot set Harvest's resumptionToken to null (purposefully
				 * throws NPE).
				 * 
				 * The token may be null in the following cases:
				 *  1. Non-list responses won't have resumption tokens.
				 *  
				 *  2. The server doesn't send back an empty token in the last
				 *     incomplete list of a list request (it's supposed to, but
				 *     some don't).
				 */
				harvest.stop();
			} else {
				final String token = resumptionToken.getToken();
				if ("".equals(token)) {
					// Harvest is done.
					harvest.stop();
				} else if (errorList.isEmpty()) {
					// If there were errors, the client may wish to retry (not
					// implemented yet).
					// Only set the token if there are none.
					harvest.setResumptionToken(resumptionToken);
				}
			}
		} catch (final XMLStreamException e) {
			tryException = new HarvesterXMLParsingException(e);
		} catch (final RuntimeException e) {
			tryException = e;
		} finally {
			if (!errorList.isEmpty()) {
				final OAIProtocolException e = new OAIProtocolException(
						errorList);
				if (tryException != null) {
					e.addSuppressed(tryException);
				}
				throw e;
			} else if (tryException != null) {
				throw tryException;
			}
		}
	}
	
	private XMLEvent nextEvent(final XMLEventReader reader,
			final OAIEventHandler eventHandler) throws XMLStreamException {
		final XMLEvent event = reader.nextEvent();
		logger.trace("Read event {}", event);
		eventHandler.onEvent(event);
		return event;
	}
	
	
	/**
	 * Read the text content of a node until non-text is seen.
	 * <p>
	 * This is used in preference to {@link XMLEventReader#getElementText()},
	 * because we want to pass all events from the input to the
	 * {@code OAIEventHandler}, and that method eats the text events.
	 * <p>
	 * There is also no mixed content in any of the nodes we care about, so this
	 * doesn't handle such cases, instead simply stopping at the first non-text
	 * node.
	 * 
	 * @param reader
	 * @param eventHandler
	 * @return
	 * @throws XMLStreamException
	 */
	private String readText(final XMLEventReader reader,
			final OAIEventHandler eventHandler) throws XMLStreamException {
		final StringBuilder sb = new StringBuilder();
		while (reader.hasNext() && reader.peek().isCharacters()) {
			final XMLEvent event = nextEvent(reader, eventHandler);
			sb.append(event.asCharacters().getData());
		}
		return sb.toString();
	}

	
	private ResumptionToken readEvents(final XMLEventReader reader,
			final List<OAIError> errorList, final Harvest harvest,
			final OAIEventHandler eventHandler)
					throws XMLStreamException {
		try {
			ResumptionToken resumptionToken = null;
			while (reader.hasNext()) {
				final XMLEvent event = nextEvent(reader, eventHandler);
				if (event.isStartElement()) {
					final StartElement startElement = event.asStartElement();
					if (isError(startElement)) {
						final String message = readText(reader, eventHandler);
						final String code = OAIXMLUtils.attributeValue(
								startElement, ERROR_CODE_ATTR);
						final OAIError error = new OAIError(code, message);
						errorList.add(error);
					} else if (isResumptionToken(startElement)) {
						final String token = readText(reader, eventHandler);
						final Long completeListSize = optionalAttributeValue(
								startElement, RT_COMPLETE_LIST_SIZE_ATTR,
								Long::parseLong);
						final Long cursor = optionalAttributeValue(startElement,
								RT_CURSOR_ATTR, Long::parseLong);
						final Instant expirationDate = optionalAttributeValue(
								startElement, RT_EXPIRATION_DATE_ATTR,
								Instant::parse);
						resumptionToken = new ResumptionToken(token,
								completeListSize, cursor, expirationDate);
					} else if (isResponseDate(startElement)) {
						final String date = readText(reader, eventHandler);
						try {
							final Instant responseDate = Instant.parse(date);
							harvest.setLastResponseDate(responseDate);
						} catch (final DateTimeParseException e) {
							logger.warn("Invalid responseDate.", e);
						}
					}
				}
			}
			return resumptionToken;
		} finally {
			OAIXMLUtils.closeQuietly(reader);
		}
	}

	private static boolean isError(final StartElement se) {
		return ERROR.equals(se.getName());
	}

	private static boolean isResumptionToken(final StartElement se) {
		return RESUMPTION_TOKEN.equals(se.getName());
	}

	private static boolean isResponseDate(final StartElement se) {
		return RESPONSE_DATE.equals(se.getName());
	}

	private <T> T optionalAttributeValue(final StartElement se, 
			final QName name, final Function<String, T> fun) {
		final String value = OAIXMLUtils.attributeValue(se, name);
		if (value != null) {
			try {
				return fun.apply(value);
			} catch (final RuntimeException e) {
				logger.warn("Exception parsing attribute " + name, e);
			}
		}
		return null;
	}
}
