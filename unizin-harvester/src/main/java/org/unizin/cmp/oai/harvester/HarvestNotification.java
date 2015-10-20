package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.ResumptionToken;

public final class HarvestNotification {
	
	public static enum HarvestNotificationType {
		HARVEST_STARTED,
		HARVEST_ENDED,
		RESPONSE_RECEIVED,
		RESPONSE_PROCESSED
	}
	
	private final HarvestNotificationType type;
	private final boolean isStarted;
	private final boolean hasError;
	private final boolean stoppedByUser;
	private final ResumptionToken resumptionToken;
	private final Instant lastResponseDate;
	private final HarvestParams params;
	private final Map<String, Long> stats;
	
	public HarvestNotification(final HarvestNotificationType type, 
			final boolean isStarted, final boolean hasError,
			final boolean stoppedByUser,
			final ResumptionToken resumptionToken, 
			final Instant lastResponseDate, final HarvestParams params,
			final Map<String, Long> stats) {
		this.type = type;
		this.isStarted = isStarted;
		this.hasError = hasError;
		this.stoppedByUser = stoppedByUser;
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
		return stoppedByUser;
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
				.append(stoppedByUser)
				.append(", resumptionToken=")
				.append(resumptionToken)
				.append(", lastResponseDate=")
				.append(lastResponseDate)
				.append("]")
				.toString();
	}
}
