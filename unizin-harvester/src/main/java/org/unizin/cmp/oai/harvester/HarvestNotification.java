package org.unizin.cmp.oai.harvester;

import java.util.Collections;
import java.util.Map;

import org.unizin.cmp.oai.ResumptionToken;

public final class HarvestNotification {
	private final boolean isStarted;
	private final boolean hasError;
	private final ResumptionToken resumptionToken;
	private final Map<String, Long> stats;
	
	public HarvestNotification(final boolean isStarted, final boolean hasError,
			final ResumptionToken resumptionToken,
			final Map<String, Long> stats) {
		this.isStarted = isStarted;
		this.hasError = hasError;
		this.resumptionToken = resumptionToken;
		this.stats = Collections.unmodifiableMap(stats);
	}
	
	public Map<String, Long> getStats() {
		return stats;
	}
	
	@Override
	public String toString() {
		return new StringBuilder(this.getClass().getName())
				.append("[")
				.append("isStarted=")
				.append(isStarted)
				.append(", hasError=")
				.append(hasError)
				.append(", resumptionToken=")
				.append(resumptionToken)
				.append("]")
				.toString();
	}
}
