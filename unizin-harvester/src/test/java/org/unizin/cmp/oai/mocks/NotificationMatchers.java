package org.unizin.cmp.oai.mocks;

import static org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType.HARVEST_ENDED;
import static org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType.HARVEST_STARTED;
import static org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType.RESPONSE_PROCESSED;
import static org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType.RESPONSE_RECEIVED;

import java.util.function.Predicate;

import org.unizin.cmp.oai.harvester.HarvestNotification;

public final class NotificationMatchers {
	public static final Predicate<HarvestNotification> RUNNING = (hn) -> {
		return hn.isStarted() && !hn.isStoppedByUser() && !hn.hasError();
	};

	public static final Predicate<HarvestNotification> HAS_ERROR = (hn) -> {
		return !hn.isStarted() && !hn.isStoppedByUser() && hn.hasError();
	};

	public static final HarvestNotification harvestStarted() {
		return OAIMatchers.fromPredicate(
				(hn) -> {
					return hn.getType() == HARVEST_STARTED &&
							NotificationMatchers.RUNNING.test(hn);
				},
				HarvestNotification.class);
	}

	public static final HarvestNotification responseReceived() {
		return OAIMatchers.fromPredicate(
				(hn) -> {
					return hn.getType() == RESPONSE_RECEIVED &&
							NotificationMatchers.RUNNING.test(hn);
				},
				HarvestNotification.class);
	}

	public static final HarvestNotification responseProcessedWithError() {
		return OAIMatchers.fromPredicate(
				(hn) -> {
					return hn.getType() == RESPONSE_PROCESSED &&
							NotificationMatchers.HAS_ERROR.test(hn);
				},
				HarvestNotification.class);
	}

	public static final HarvestNotification responseProcessedSuccessfully() {
		return OAIMatchers.fromPredicate(
				(hn) -> {
					return hn.getType() == RESPONSE_PROCESSED &&
							NotificationMatchers.RUNNING.test(hn);
				},
				HarvestNotification.class);
	}
	
	public static final HarvestNotification harvestEndedWithError() {
		return OAIMatchers.fromPredicate(
				(hn) -> {
					return hn.getType() == HARVEST_ENDED &&
							NotificationMatchers.HAS_ERROR.test(hn);
				},
				HarvestNotification.class);
	}
	
	public static final HarvestNotification harvestEndedSuccessfully() {
		return OAIMatchers.fromPredicate(
				(hn) -> {
					return hn.getType() == HARVEST_ENDED &&
							NotificationMatchers.RUNNING.test(hn);
				},
				HarvestNotification.class);		
	}

	/** No instances allowed. */
	private NotificationMatchers() {}
}
