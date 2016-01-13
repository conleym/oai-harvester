package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.http.client.methods.HttpUriRequest;
import org.unizin.cmp.oai.OAI2Constants;
import org.unizin.cmp.oai.OAIRequestParameter;
import org.unizin.cmp.oai.ResumptionToken;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestStatistic;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;

/**
 * Internal-use-only mutable harvest state.
 *
 */
final class Harvest {

    static final class State {
        volatile boolean running;
        volatile boolean explicitlyStopped;
        volatile boolean cancelled;
        boolean interrupted;
    }

    private final HarvestParams params;
    private final OAIResponseHandler responseHandler;
    private final Map<String, String> tags;
    private final State state = new State();
    private HttpUriRequest request;
    private SortedMap<String, String> requestParams;
    private Exception exception;
    /**
     * The resumption token from the last response, if any.
     * <p>
     * Must be {@code volatile} so that {@link #getRetryParams()} can be called
     * from multiple threads.
     * </p>
     */
    private volatile ResumptionToken resumptionToken;
    private Instant lastResponseDate;
    private Instant started;
    private Instant ended;
    private long requestCount;
    private long responseCount;
    private long xmlEventCount;


    Harvest() {
        this(null, null, Collections.emptyMap());
    }

    Harvest(final HarvestParams params,
            final OAIResponseHandler responseHandler,
            final Map<String, String> tags) {
        this.params = params;
        this.responseHandler = responseHandler;
        this.tags = Collections.unmodifiableMap(new TreeMap<>(tags));
    }

    HarvestNotification createNotification(
            final HarvestNotificationType type) {
        final Map<HarvestStatistic, Long> stats = new HashMap<>(2);
        stats.put(HarvestStatistic.REQUEST_COUNT, requestCount);
        stats.put(HarvestStatistic.RESPONSE_COUNT, responseCount);
        stats.put(HarvestStatistic.XML_EVENT_COUNT, xmlEventCount);
        final URI uri = (request == null) ? null : request.getURI();
        return new HarvestNotification(type, tags, state, exception,
                resumptionToken, lastResponseDate, params, stats,
                uri, requestParams, started, ended);
    }

    void setLastResponseDate(final Instant lastResponseDate) {
        this.lastResponseDate = lastResponseDate;
    }

    void setResumptionToken(final ResumptionToken resumptionToken) {
        Objects.requireNonNull(resumptionToken, "resumptionToken");
        this.resumptionToken = resumptionToken;
    }

    void setRequest(final HttpUriRequest request) {
        this.request = request;
    }

    HttpUriRequest getRequest() {
        return request;
    }

    /**
     * Get the parameters for the next request.
     * @return the parameters for the next request.
     */
    Map<String, String> getRequestParameters() {
        if (resumptionToken != null) {
            final Map<String, String> m = new HashMap<>();
            m.put(OAIRequestParameter.RESUMPTION_TOKEN.paramName(),
                    resumptionToken.getToken());
            m.put(OAI2Constants.VERB_PARAM_NAME,
                    params.getVerb().localPart());
            requestParams = new TreeMap<>(m);
        } else {
            requestParams = params.getParameters();
        }
        return requestParams;
    }

    URI getBaseURI() {
        return params.getBaseURI();
    }

    void start() {
        state.running = true;
        started = Instant.now();
    }

    void stop() {
        state.running = false;
        ended = Instant.now();
    }

    void cancel() {
        state.cancelled = true;
        requestStop();
    }

    boolean isCancelled() {
        return state.cancelled;
    }

    /**
     * This method should be called only from {@link Harvester#stop()}.
     */
    void requestStop() {
        state.explicitlyStopped = true;
        stop();
    }

    void error(final Exception e) {
        exception = e;
        stop();
    }

    void requestSent() {
        requestCount++;
    }

    void responseReceived() {
        responseCount++;
    }

    void xmlEventReceived() {
        xmlEventCount++;
    }

    boolean hasNext() {
        /* On the off chance that somebody else down the line is interested
         * in the interrupted flag, we avoid clearing it.
         */
        if (Thread.currentThread().isInterrupted()) {
            state.interrupted = true;
            stop();
        }
        return state.running;
    }

    HarvestParams getRetryParams() {
        if (params != null) {
            return params.getRetryParameters(resumptionToken);
        }
        throw new IllegalStateException("No current harvest parameters.");
    }

    HarvestParams getHarvestParams() {
        if (params != null) {
            return params;
        }
        throw new IllegalStateException("No current harvest parameters.");
    }

    OAIResponseHandler getResponseHandler() {
        return responseHandler;
    }

    OAIEventHandler getEventHandler(final HarvestNotification notification) {
        return responseHandler.getEventHandler(notification);
    }
}

