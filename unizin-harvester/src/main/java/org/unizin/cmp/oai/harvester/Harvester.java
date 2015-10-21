package org.unizin.cmp.oai.harvester;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.OAIXMLUtils;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;

/**
 * An OAI-PMH 2.0 <a href=
 * "http://www.openarchives.org/OAI/openarchivesprotocol.html#harvester">
 * harvester</a>.
 * <p>
 * Each instance is a wrapper around an instance of {@link HttpClient}, together
 * with a mutable state object representing the state of the current harvest.
 * <p>
 * Instances are neither immutable nor safe for use in multiple threads.
 */
public final class Harvester extends Observable {
	private static final Logger LOGGER = 
			LoggerFactory.getLogger(Harvester.class);


	public static final class Builder {
		private HttpClient httpClient;
		private OAIRequestFactory requestFactory = 
				GetOAIRequestFactory.getInstance();
		private XMLInputFactory inputFactory;

		public Builder withHttpClient(final HttpClient httpClient) {
			this.httpClient = httpClient;
			return this;
		}

		public Builder withXMLInputFactory(final XMLInputFactory inputFactory) {
			this.inputFactory = inputFactory;
			return this;
		}

		public Builder withOAIRequestFactory(
				final OAIRequestFactory requestFactory) {
			this.requestFactory = requestFactory;
			return this;
		}

		public Harvester build() {
			if (httpClient == null) {
				httpClient = HttpClients.createDefault();
			}
			if (inputFactory == null) {
				inputFactory = OAIXMLUtils.newInputFactory();
			}
			return new Harvester(httpClient, requestFactory, inputFactory);
		}
	}


	private final class HarvestIterable implements Iterable<InputStream> {
		@Override
		public Iterator<InputStream> iterator() {
			return new Iterator<InputStream>() {
				@Override
				public boolean hasNext() {
					return harvest.hasNext();
				}

				@Override
				public InputStream next() {
					final Map<String, String> parameters =
							harvest.getRequestParameters();
					final HttpResponse response = executeRequest(
							createRequest(parameters));
					return contentOf(response);
				}
			};
		}
	}

	private final HttpClient httpClient;
	private final OAIRequestFactory requestFactory;
	private final OAIResponseParser responseParser;

	private Harvest harvest;
	private OAIResponseHandler responseHandler;

	public Harvester(final HttpClient httpClient,
			final OAIRequestFactory requestFactory,
			final XMLInputFactory inputFactory) {
		Objects.requireNonNull(httpClient, "httpClient");
		Objects.requireNonNull(requestFactory, "requestFactory");
		Objects.requireNonNull(inputFactory, "inputFactory");
		requireNamespaceAware(inputFactory);
		this.httpClient = httpClient;
		this.requestFactory = requestFactory;
		this.responseParser = new OAIResponseParser(inputFactory, LOGGER);
	}

	private static void requireNamespaceAware(
			final XMLInputFactory inputFactory) {
		final Object val = inputFactory.getProperty(
				XMLInputFactory.IS_NAMESPACE_AWARE);
		if (! (val instanceof Boolean && ((Boolean)val))) {
			throw new IllegalArgumentException(
					"XMLInputFactory must be namespace-aware.");
		}
	}

	public void start(final HarvestParams params,
			final OAIResponseHandler responseHandler) {
		this.harvest = new Harvest(params);
		this.responseHandler = responseHandler;
		harvest();
	}

	/**
	 * Stop the current harvest, if any.
	 * <p>
	 * This method is safe to call from multiple threads.
	 */
	public void stop() {
		if (harvest != null) {
			harvest.userStop();
		}
	}

	/**
	 * Safely run some code requiring a {@code finally} block without losing
	 * exceptions.
	 * <p>
	 * Suppose an exception, {@code E}, is thrown in a {@code try} block. We
	 * need to run some code in the corresponding {@code finally} block that may
	 * <em>also</em> throw an exception, {@code F}, but we don't want to lose
	 * {@code E}.
	 * <p>
	 * There are four possibilities to consider:
	 * <ol>
	 * <li>Neither {@code E} nor {@code F} is thrown. This method will not
	 * throw.
	 * <li>{@code E} and {@code F} are both thrown. This method will throw
	 * {@code E}, with {@code F} attached as a suppressed exception.
	 * <li>Only {@code E} is thrown. This method will throw {@code E}.
	 * <li>Only {@code F} is thrown. This method will throw {@code F}.
	 * </ol>
	 * 
	 * @param tryCall
	 *            the code to run inside a {@code try} block.
	 * @param finallyCall
	 *            the code to run inside the corresponding {@code finally}
	 *            block.
	 */
	private void suppressHandlerExceptionsInCall(final Runnable tryCall,
			final Runnable finallyCall) {
		RuntimeException caught = null;
		try {
			tryCall.run();
		} catch (final RuntimeException bodyEx) {
			caught = bodyEx;
		} finally {
			try {
				finallyCall.run();
			} catch (final RuntimeException finallyEx) {
				if (caught == null) {
					throw finallyEx;
				} else {
					caught.addSuppressed(finallyEx);
					throw caught;
				}
			}
			/*
			 * Finally call had no exceptions. Might still have to throw body
			 * exception.
			 */
			if (caught != null) {
				throw caught;
			}
		}
	}
	
	private void harvest() {
		suppressHandlerExceptionsInCall(this::harvestLoop, 
				this::sendHarvestEndNotifications);
	}

	private void harvestLoop() {
		harvest.start();
		sendHarvestStartNotifications();
		final HarvestIterable iterable = new HarvestIterable();
		for (final InputStream is : iterable) {
			suppressHandlerExceptionsInCall(() -> handleResponse(is),
					this::sendResponseEndNotifications);
		}
	}
	
	private void handleResponse(final InputStream is) {
		try (final InputStream in = is) { // Make sure streams get closed.
			harvest.responseReceived();
			final HarvestNotification notification = 
					harvest.createNotification(
							HarvestNotificationType.RESPONSE_RECEIVED);
			sendToObservers(notification);
			responseHandler.onResponseReceived(notification);
			responseParser.parse(in, harvest, 
					responseHandler.getEventHandler(notification));
		} catch (final HarvesterException e) {
			harvest.error();
			throw e;
		} catch (final XMLStreamException | IOException e) {
			/*
			 * Note: XMLStreamExceptions thrown due to XML parsing
			 * errors have already been caught and wrapped inside the
			 * parser.
			 * 
			 * IOException can only be thrown when closing one of the
			 * streams.
			 */
			harvest.error();
			throw new HarvesterException(e);
		}
	}

	private HttpUriRequest createRequest(final Map<String, String> parameters) {
		final HttpUriRequest request = requestFactory.createRequest(
				harvest.getBaseURI(), parameters);
		LOGGER.trace("Request created: {}", request);
		return request;
	}

	private HttpResponse executeRequest(final HttpUriRequest request) {
		harvest.setRequest(request);
		harvest.requestSent();
		try {
			LOGGER.debug("Executing request {}", request);
			final HttpResponse response = httpClient.execute(
					request);
			LOGGER.debug("Got HTTP response {} for request {}",
					response, request);
			return response;
		} catch (final IOException e) {
			throw new HarvesterException(e);
		}
	}
	

	/**
	 * Get the content of an {@code HttpResponse}.
	 * 
	 * @param response
	 *            the HTTP response.
	 * @return the content of the response's entity.
	 * @throws HarvesterException
	 *             if the response's status code is not OK, the response's
	 *             entity is {@code null}, or if there's an error getting the
	 *             entity's content.
	 */
	private InputStream contentOf(final HttpResponse response) {
		try {
			final StatusLine statusLine = response.getStatusLine();
			LOGGER.debug("Got status line: {}", statusLine);
			final int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				return entity(response).getContent();
			}
			throw new HarvesterException(String.format(
					"Got HTTP status %d for request %s.",
					statusCode,
					harvest.getRequest()));
		} catch (final IllegalStateException | IOException e) {
			throw new HarvesterException(e);
		}
	}

	private HttpEntity entity(final HttpResponse response) {
		final HttpEntity entity = response.getEntity();
		if (entity == null) {
			throw new HarvesterException(
					String.format("Got null HTTP entity in response to request %s.",
							harvest.getRequest()));
		}
		return entity;
	}
	
	private void sendHarvestStartNotifications() {
		final HarvestNotification notification = harvest.createNotification(
				HarvestNotificationType.HARVEST_STARTED);
		responseHandler.onHarvestStart(notification);
		sendToObservers(notification);
	}
	
	private void sendHarvestEndNotifications() {
		final HarvestNotification notification = harvest.createNotification(
				HarvestNotificationType.HARVEST_ENDED);
		responseHandler.onHarvestEnd(notification);
		sendToObservers(notification);
	}
	
	private void sendResponseEndNotifications() {
		final HarvestNotification notification = 
				harvest.createNotification(
						HarvestNotificationType.RESPONSE_PROCESSED);
		responseHandler.onResponseProcessed(notification);
		sendToObservers(notification);
	}
	
	private void sendToObservers(final HarvestNotification notification) {
		setChanged();
		notifyObservers(notification);
	}
}

