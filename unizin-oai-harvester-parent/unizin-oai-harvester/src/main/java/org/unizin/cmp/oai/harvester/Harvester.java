package org.unizin.cmp.oai.harvester;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;
import java.util.function.Consumer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;
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
 * </p>
 * <p>
 * Instances are mutable, but public methods can be safely called from multiple
 * threads.
 * </p>
 * <h2>Use in Multiple Threads</h2>
 * <p>
 * The harvester itself does not start or manage threads, leaving this up to the
 * client. Harvester instances can be used in multiple threads by creating a
 * harvester in one thread and running it in another, as in the following simple
 * example:
 * </p>
 * <pre>
 *   // This is the main thread.
 *
 *   // Customize builder to taste.
 *   final Harvester har = new Harvester.Builder().build();
 *   // Define your parameters....
 *   final HarvesterParams params = ...;
 *   Thread t = new Thread(() -&gt; {
 *      // Harvest runs in this thread.
 *      OAIResponseHandler handler = ...;
 *      har.start(params, handler);
 *   });
 *   t.start();
 * </pre>
 * <p>
 * If desired, one can call {@code t.interrupt()} or {@code har.stop()} to stop
 * the harvest from the main thread. Once either of these steps are taken, the
 * harvest will stop after the current response has been fully processed. To
 * stop the harvest more quickly, one could instead call {@code har.cancel()},
 * which will tell {@code har} to stop the next time it looks for more XML to
 * read from the current response.
 * </p>
 * <p>
 * In this simple example, the response handler is not shared between threads,
 * but the harvester's design allows for shared handlers, provided the handlers
 * themselves are safe for use in multiple threads.
 * </p>
 */
public final class Harvester extends Observable {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(Harvester.class);

    /**
     * Default socket timeout (the amount of time {@code HttpClient} will wait
     * between packets) used when building a default {@code HttpClient}, in
     * milliseconds.
     *
     * @see RequestConfig#getSocketTimeout()
     * @see SocketConfig#getSoTimeout()
     */
    private static final int DEFAULT_SO_TIMEOUT = 100;

    /**
     * Connection request timeout (the amount of time {@code HttpClient} will
     * wait for a connection from the pool) used when building a default
     * {@code HttpClient}, in milliseconds.
     *
     * @see RequestConfig#getConnectionRequestTimeout()
     */
    private static final int DEFAULT_CONN_REQ_TIMEOUT = 100;

    /**
     * Connection timeout (the amount of time {@code HttpClient} will wait for a
     * connection to be established) used when building a default
     * {@code HttpClient}, in milliseconds.
     *
     * @see RequestConfig#getConnectTimeout()
     */
    private static final int DEFAULT_CONNECT_TIMEOUT = 100;

    /**
     * Create an HTTP client builder instance.
     *
     * @return a builder with configured socket, connect, and connection request
     *         timeouts.
     */
    public static HttpClientBuilder defaultHttpClient() {
        /*
         * Socket timeout is set in both places where we can set it. It looks
         * like RequestConfig's value takes priority, but it's hard to be sure.
         */
        final SocketConfig config = SocketConfig.custom()
                .setSoTimeout(DEFAULT_SO_TIMEOUT)
                .build();
        final RequestConfig rc = RequestConfig.custom()
                .setConnectionRequestTimeout(DEFAULT_CONN_REQ_TIMEOUT)
                .setConnectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .setSocketTimeout(DEFAULT_SO_TIMEOUT)
                .build();
        // TODO add 503 handling when httpclient changes to respect the retry-after header.
        return HttpClients.custom()
                .setDefaultSocketConfig(config)
                .setDefaultRequestConfig(rc);
    }


    public static final class Builder {
        private HttpClient httpClient;
        private OAIRequestFactory requestFactory =
                GetOAIRequestFactory.getInstance();
        private XMLInputFactory inputFactory;

        /**
         * Set the {@code HttpClient} to use.
         * <p>
         * It is <em>strongly</em> recommended that this client have reasonable
         * timeouts set. Note that the default configuration (created by e.g.,
         * {@code HttpClients.createDefault()}) has no timeout.
         * </p>
         * <p>
         * If this method is not called, the resulting harvester will use a
         * default like that provided by {@link HttpClients#createDefault()},
         * but with default timeouts configured.
         * </p>
         *
         * @param httpClient
         *            the HTTP client instance the resulting harvester will use
         *            to execute all its requests.
         * @return this builder.
         */
        public Builder withHttpClient(final HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Set the {@code XMLInputFactory} to use.
         * <p>
         * If this method is not called, the resulting harvester will use a
         * default provided by {@link OAIXMLUtils#newInputFactory()}.
         *
         * @param inputFactory
         *            the XML input factory to use. This factory <em>must</em>
         *            be namespace-aware. In addition, it is <em>highly</em>
         *            recommended that DTDs be disabled for security reasons.
         *            See {@link OAIXMLUtils#newInputFactory()} for details.
         * @return this builder.
         */
        public Builder withXMLInputFactory(final XMLInputFactory inputFactory) {
            this.inputFactory = inputFactory;
            return this;
        }

        /**
         * Set the {@code OAIRequestFactory} to use.
         * <p>
         * If this method isn't called, the resulting harvester will use
         * {@link GetOAIRequestFactory}.
         *
         * @param requestFactory
         *            the request factory that will be used to make
         *            {@code HttpUriRequest} objects for each request the
         *            resulting harvester makes.
         * @return this builder.
         */
        public Builder withOAIRequestFactory(
                final OAIRequestFactory requestFactory) {
            this.requestFactory = requestFactory;
            return this;
        }

        public Harvester build() {
            if (httpClient == null) {
                httpClient = defaultHttpClient().build();
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
                    try {
                        final Map<String, String> parameters =
                                harvest.getRequestParameters();
                        final HttpResponse response = executeRequest(
                                createRequest(parameters));
                        return contentOf(response);
                    } catch (final RuntimeException e) {
                        harvest.error(e);
                        throw e;
                    }
                }
            };
        }
    }


    private final HttpClient httpClient;
    private final OAIRequestFactory requestFactory;
    private final OAIResponseParser responseParser;

    /**
     * The current harvest state.
     * <p>
     * Must be {@code volatile} so that a harvest can be started via
     * {@link #start(HarvestParams, OAIResponseHandler)} called from one thread
     * and stopped via {@link #stop()} called from another.
     * </p>
     */
    // Initial value has false for all flags and cannot be restarted.
    private volatile Harvest harvest = new Harvest();

    /**
     * Create a new instance.
     *
     * @param httpClient
     *            the HTTP client to use. It is <em>strongly</em>
     *            recommended that this client have reasonable timeouts set.
     *            Note that the default configuration (created by e.g.,
     *            {@code HttpClients#createDefault()}) has no timeouts.
     * @param requestFactory
     *            the request factory to use.
     * @param inputFactory
     *            the XML input factory to use. This factory <em>must</em> be
     *            namespace-aware. In addition, it is <em>highly</em>
     *            recommended that DTDs be disabled for security reasons. See
     *            {@link OAIXMLUtils#newInputFactory()} for details.
     *
     * @throws NullPointerException
     *             if any of the arguments are {@code null}.
     * @throws IllegalArgumentException
     *             if the XML input factory is not namespace-aware.
     */
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
        if (!(val instanceof Boolean && ((Boolean) val))) {
            throw new IllegalArgumentException(
                    "XMLInputFactory must be namespace-aware.");
        }
    }

    /**
     * Start a new harvest with no tags.
     * <p>
     * Equivalent to {@code start(params, responseHandler,
     * Collections.emptyMap())}.
     * </p>
     * @see #start(HarvestParams, OAIResponseHandler, Map)
     */
    public void start(final HarvestParams params,
            final OAIResponseHandler responseHandler) {
        start(params, responseHandler, Collections.emptyMap());
    }

    /**
     * Start a new harvest.
     *
     * @param params
     *            the harvest parameters.
     * @param responseHandler
     *            the handler to use to deal with server responses.
     * @param tags
     *            data associated with this harvest. The given map will be
     *            copied, and that copy attached to each notification produced
     *            by this harvest.
     *
     * @throws UncheckedIOException
     *             if there's an {@code IOException} while executing an HTTP
     *             request with {@link HttpClient#execute(HttpUriRequest)}.
     * @throws HarvesterException
     *             if there's an error harvesting.
     * @throws IllegalStateException
     *             if this method is called while another harvest is already in
     *             progress.
     */
    public void start(final HarvestParams params,
            final OAIResponseHandler responseHandler,
            final Map<String, String> tags) {
        if (this.harvest.hasNext()) {
            throw new IllegalStateException(
                    "Cannot start a new harvest while one is in progress.");
        }
        this.harvest = new Harvest(params, responseHandler, tags);
        harvest();
    }

    /**
     * Stop the current harvest, if any, after the current response is fully
     * processed.
     * <p>
     * Note that the current harvest might not stop immediately. Clients
     * wishing to take action when a harvest ends should add observers to be
     * notified of events.
     * </p>
     */
    public void stop() {
        harvest.requestStop();
    }

    /**
     * Stop the current harvest, if any, as soon as possible.
     * <p>
     * This stops the harvest without regard for any ongoing processing.
     * Depending upon guarantees made by response handlers, the results might
     * not be valid.
     * </p>
     * <p>
     * Note that the current harvest might not stop immediately. Clients
     * wishing to take action when a harvest ends should add observers to be
     * notified of events.
     * </p>
     */
    public void cancel() {
        harvest.cancel();
    }

    /**
     * Get the parameters needed to retry the harvest, starting with the most
     * recent request.
     * <p>
     * These parameters can be passed to
     * {@link #start(HarvestParams, OAIResponseHandler)} to start a new harvest.
     * Note that this is a <em>new</em> harvest. In particular, statistics about
     * the current harvest will be lost.
     * </p>
     *
     * @return the parameters needed to retry the most recent request.
     * @throws IllegalStateException
     *             if there is no current harvest.
     */
    public HarvestParams getRetryParams() {
        return harvest.getRetryParams();
    }

    private void harvest() {
        Functions.suppressExceptions(this::harvestLoop,
                this::sendHarvestEndNotifications);
    }

    private void harvestLoop() {
        harvest.start();
        sendHarvestStartNotifications();
        final HarvestIterable iterable = new HarvestIterable();
        for (final InputStream is : iterable) {
            Functions.suppressExceptions(() -> handleResponse(is),
                    this::sendResponseEndNotifications);
        }
    }

    /**
     * Handles a single response from a repository, parsing its content and
     * triggering appropriate events.
     *
     * @param is
     *            the content of the response. The stream will be closed by this
     *            method.
     */
    private void handleResponse(final InputStream is) {
        try (final InputStream in = is) { // Make sure streams get closed.
            harvest.responseReceived();
            final HarvestNotification notification =
                    sendResponseReceivedNotifcations();
            responseParser.parse(in, harvest,
                    harvest.getEventHandler(notification));
        } catch (final XMLStreamException | IOException e) {
            /*
             * Note: XMLStreamExceptions thrown due to XML parsing
             * errors have already been caught and wrapped inside the
             * parser.
             *
             * IOException can only be thrown when closing the stream.
             */
            harvest.error(e);
            throw new HarvesterException(e);
        } catch (final RuntimeException e) {
            /*
             * Make sure anybody who's listening for notifications knows there
             * was an error.
             */
            harvest.error(e);
            throw e;
        }
    }

    private HttpUriRequest createRequest(final Map<String, String> parameters) {
        final HttpUriRequest request = requestFactory.createRequest(
                harvest.getBaseURI(), parameters);
        LOGGER.trace("Request created: {}", request);
        return request;
    }

    /**
     * Execute an {@code HttpUriRequest}.
     *
     * @param request
     *            the request to execute.
     * @return the server's response.
     * @throws UncheckedIOException
     *             if {@link HttpClient#execute(HttpUriRequest)} throws an
     *             {@link IOException}.
     */
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
            throw new UncheckedIOException(e);
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
            if (statusCode == HttpStatus.SC_OK) {
                return entity(response).getContent();
            }
            throw new HarvesterException(String.format(
                    "Got HTTP status %d for request %s.",
                    statusCode,
                    harvest.getRequest()));
        } catch (final HarvesterException e) {
            // Avoid wrapping the exception we just threw.
            throw e;
        } catch (final RuntimeException | IOException e) {
            throw new HarvesterException(e);
        }
    }

    /**
     * Get the entity from a response.
     *
     * @param response
     *            the response from which the entity is to be extracted.
     * @return the response's entity.
     * @throws HarvesterException
     *             if the response has no entity.
     */
    private HttpEntity entity(final HttpResponse response) {
        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            throw new HarvesterException(
                    String.format(
                            "Got null HTTP entity in response to request %s.",
                            harvest.getRequest()));
        }
        return entity;
    }

    /**
     * Sends a notification first to a consumer (one of the
     * {@link OAIResponseHandler} event notification methods), then to
     * registered observers.
     * <p>
     * This method makes the following guarantees:
     * <ol>
     * <li>The consumer receives the notification first.</li>
     * <li>If the consumer throws an exception, this method will still attempt
     * to notify all observers. The exception thrown by the consumer will be
     * thrown after this attempt is made, with any exception thrown while
     * attempting to notify observers being logged and <em>not</em> thrown.
     * </li>
     * </ol>
     * </p>
     *
     * @param notification
     *            the notification to send.
     * @param consumer
     *            the consumer that should receive the notification before
     *            registered observers.
     */
    private void sendNotifications(final HarvestNotification notification,
            final Consumer<HarvestNotification> consumer) {
        try {
            consumer.accept(notification);
        } finally {
            sendToObservers(notification);
        }
    }

    /**
     * Send a {@link HarvestNotificationType#HARVEST_STARTED} notification to
     * the harvest's response handler and to registered observers.
     */
    private void sendHarvestStartNotifications() {
        final HarvestNotification notification = harvest.createNotification(
                HarvestNotificationType.HARVEST_STARTED);
        sendNotifications(notification,
                harvest.getResponseHandler()::onHarvestStart);
    }

    /**
     * Send a {@link HarvestNotificationType#HARVEST_ENDED} notification to the
     * harvest's response handler and to registered observers.
     */
    private void sendHarvestEndNotifications() {
        final HarvestNotification notification = harvest.createNotification(
                HarvestNotificationType.HARVEST_ENDED);
        sendNotifications(notification,
                harvest.getResponseHandler()::onHarvestEnd);
    }

    /**
     * Send a {@link HarvestNotificationType#RESPONSE_RECEIVED} notification to
     * the harvest's response handler and to registered observers.
     */
    private HarvestNotification sendResponseReceivedNotifcations() {
        final HarvestNotification notification = harvest.createNotification(
                HarvestNotificationType.RESPONSE_RECEIVED);
        sendNotifications(notification,
                harvest.getResponseHandler()::onResponseReceived);
        return notification;
    }

    /**
     * Send a {@link HarvestNotificationType#RESPONSE_PROCESSED} notification to
     * the harvest's response handler and to registered observers.
     */
    private void sendResponseEndNotifications() {
        final HarvestNotification notification = harvest.createNotification(
                HarvestNotificationType.RESPONSE_PROCESSED);
        sendNotifications(notification,
                harvest.getResponseHandler()::onResponseProcessed);
    }

    /**
     * Send a notification to registered observers.
     *
     * <h2>Error Handling</h2>
     * <p>
     * Observers should not throw exceptions when receiving updates (by
     * convention). If an observer should defy convention and throw, we want to
     * ensure that the operation of the harvester is not disrupted. Thus, this
     * method does not throw any exceptions. Any exceptions thrown by observers
     * will be logged.
     * </p>
     * <p>
     * Because {@code Observable} makes no guarantees about notification order,
     * we cannot either. Nor can we guarantee that an attempt is made to notify
     * every registered observer if any one of them throws while being updated.
     * </p>
     *
     * @param notification
     *            the notification to send.
     */
    private void sendToObservers(final HarvestNotification notification) {
        setChanged();
        try {
            notifyObservers(notification);
        } catch (final Exception e) {
            LOGGER.error("Caught an exception while notifying observers.",
                    e);
        }
    }
}

