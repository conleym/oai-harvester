package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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
	private boolean isStarted;
	private volatile boolean stoppedByUser;
	private boolean hasError;
	private ResumptionToken resumptionToken;
	private Instant lastResponseDate;
	private long partialListResponseCount;
	
	Harvest(final HarvestParams params) {
		this.params = params;
	}
	
	HarvestNotification createNotification(final HarvestNotificationType type) {
		final Map<String, Long> stats = new HashMap<>(1);
		stats.put("partialListResponseCount", partialListResponseCount);
		return new HarvestNotification(type, isStarted, hasError, stoppedByUser,
				resumptionToken, lastResponseDate, params, stats);
	}
	
	void setLastResponseDate(final Instant lastResponseDate) {
		this.lastResponseDate = lastResponseDate;
	}
	
	void setResumptionToken(final ResumptionToken resumptionToken) {
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
		this.stoppedByUser = true;
	}
	
	void error() {
		hasError = true;
	}
	
	void partialResponseRecieved() {
        partialListResponseCount++;
	}
	
	boolean hasNext() {
		return isStarted && !hasError && !stoppedByUser;
	}
}

