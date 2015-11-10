package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.http.client.methods.HttpUriRequest;
import org.unizin.cmp.oai.OAI2Constants;
import org.unizin.cmp.oai.OAIRequestParameter;
import org.unizin.cmp.oai.ResumptionToken;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType;

/**
 * Internal-use-only mutable harvest state.
 *
 */
final class Harvest {
    private final HarvestParams params;
    private HttpUriRequest request;
    private volatile boolean isStarted;
    private volatile boolean isStoppedByUser;
    private boolean hasError;
    private ResumptionToken resumptionToken;
    private Instant lastResponseDate;
    private long requestCount;
    private long responseCount;


    Harvest(final HarvestParams params) {
        this.params = params;
    }

    HarvestNotification createNotification(final HarvestNotificationType type) {
        final Map<String, Long> stats = new HashMap<>(2);
        stats.put(HarvestNotification.Statistics.REQUEST_COUNT, requestCount);
        stats.put(HarvestNotification.Statistics.RESPONSE_COUNT,
                responseCount);
        return new HarvestNotification(type, isStarted, hasError, isStoppedByUser,
                resumptionToken, lastResponseDate, params, stats);
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
            return m;
        }
        return params.getParameters();
    }

    URI getBaseURI() {
        return params.getBaseURI();
    }

    void start() {
        isStarted = true;
    }

    void stop() {
        isStarted = false;
    }

    /**
     * This method should be called only from {@link Harvester#stop()}.
     */
    void userStop() {
        this.isStoppedByUser = true;
        stop();
    }

    void error() {
        hasError = true;
        stop();
    }

    void requestSent() {
        requestCount++;
    }

    void responseReceived() {
        responseCount++;
    }

    boolean hasNext() {
        return isStarted && !hasError && !isStoppedByUser;
    }

    HarvestParams getRetryParams() {
        return params.getRetryParameters(resumptionToken);
    }
}

