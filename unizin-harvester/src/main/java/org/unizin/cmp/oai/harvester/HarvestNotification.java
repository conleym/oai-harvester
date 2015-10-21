package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.ResumptionToken;

/**
 * Immutable snapshot of harvest state.
 *
 */
public final class HarvestNotification {
	
	public static enum HarvestNotificationType {
		HARVEST_STARTED,
		HARVEST_ENDED,
		RESPONSE_RECEIVED,
		RESPONSE_PROCESSED
	}
	
	public static final class Statistics {
		public static final String REQUEST_COUNT = "requestCount";
		public static final String RESPONSE_COUNT = "responseCount";
		
		/** No instances allowed. */
		private Statistics() {}
	}
	
	private final HarvestNotificationType type;
	private final boolean isStarted;
	private final boolean hasError;
	private final boolean isStoppedByUser;
	private final ResumptionToken resumptionToken;
	private final Instant lastResponseDate;
	private final HarvestParams params;
	private final Map<String, Long> stats;
	
	public HarvestNotification(final HarvestNotificationType type, 
			final boolean isStarted, final boolean hasError,
			final boolean isStoppedByUser,
			final ResumptionToken resumptionToken, 
			final Instant lastResponseDate, final HarvestParams params,
			final Map<String, Long> stats) {
		this.type = type;
		this.isStarted = isStarted;
		this.hasError = hasError;
		this.isStoppedByUser = isStoppedByUser;
		this.resumptionToken = resumptionToken;
		this.lastResponseDate = lastResponseDate;
		this.params = params;
		this.stats = Collections.unmodifiableMap(stats);
	}
	
	public Map<String, Long> getStats() {
		return stats;
	}
	
	public URI getBaseURI() {
		return params.getBaseURI();
	}
	
	public OAIVerb getVerb() {
		return params.getVerb();
	}
	
	public HarvestNotificationType getType() {
		return type;
	}
	
	public boolean isStoppedByUser() {
		return isStoppedByUser;
	}
	
	public boolean hasError() {
		return hasError;
	}
	
	public boolean isStarted() {
		return isStarted;
	}
	
	
	@Override
	public String toString() {
		return new StringBuilder(this.getClass().getName())
				.append("[type=")
				.append(type)
				.append(", isStarted=")
				.append(isStarted)
				.append(", hasError=")
				.append(hasError)
				.append(", stoppedByUser=")
				.append(isStoppedByUser)
				.append(", resumptionToken=")
				.append(resumptionToken)
				.append(", lastResponseDate=")
				.append(lastResponseDate)
				.append(", stats=")
				.append(stats)
				.append("]")
				.toString();
	}
}
