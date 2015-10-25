package org.unizin.cmp.oai.harvester;

import static org.mockito.Matchers.eq;
import static org.unizin.cmp.oai.mocks.Mocks.inOrderVerify;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Observer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;
import org.unizin.cmp.oai.mocks.Mocks;
import org.unizin.cmp.oai.mocks.NotificationMatchers;
import org.unizin.cmp.oai.templates.ListRecordsTemplate;
import org.unizin.cmp.oai.templates.RecordMetadataTemplate;

import freemarker.template.TemplateException;

public final class TestListResponses extends HarvesterTestBase {
	@Rule
	public ExpectedException exception = ExpectedException.none();

	/**
	 * Set up with two incomplete lists. First has two records, second has one.
	 * 
	 * @param sendFinalResumptionToken
	 *            should we follow the standard and send an empty resumption
	 *            token in the last incomplete list? If {@code false}, do what
	 *            many repositories actually do, and send no token at all.
	 */
	private void setupWithDefaultListRecordsResponse(
			final boolean sendFinalResumptionToken) 
					throws TemplateException, IOException{
		final String tokenString = "the response token";
		final Map<String, Object> token = new HashMap<>();
		final Map<String, Object> record = new HashMap<>();

		token.put("token", tokenString);
		token.put("cursor", "2");
		token.put("completeListSize", "3");
		ListRecordsTemplate listRecordsTemplate = new ListRecordsTemplate()
				.withResumptionToken(token);
		RecordMetadataTemplate recordTemplate = new RecordMetadataTemplate()
				.addTitle("A Title")
				.addCreator("Some Creator")
				.addCreator("Another Creator");
		record.put("metadata", recordTemplate.process());
		listRecordsTemplate.addRecord(record);
		recordTemplate = new RecordMetadataTemplate()
				.addTitle("Another Title")
				.addTitle("Yet More Title")
				.addDate("2015-10-31")
				.addDate("1900-01-01");
		record.clear();
		record.put("metadata", recordTemplate.process());
		listRecordsTemplate.addRecord(record);			
		mockHttpClient.addResponseFrom(200, "", listRecordsTemplate.process());

		token.clear();
		token.put("token", "");
		token.put("cursor", "3");
		token.put("completeListSize", "3");
		listRecordsTemplate = new ListRecordsTemplate()
				.withResumptionToken(token);
		recordTemplate = new RecordMetadataTemplate()
				.addTitle("Such Title Wow");
		record.clear();
		record.put("metadata", recordTemplate.process());
		listRecordsTemplate.addRecord(record);
		mockHttpClient.addResponseFrom(200, "", listRecordsTemplate.process());
	}

	private void listRecordsTest(final long totalResponses)
			throws TemplateException, IOException {
		final Harvester harvester = defaultTestHarvester();
		final OAIResponseHandler h = Mocks.newResponseHandler();
		final Observer obs = Mockito.mock(Observer.class);
		harvester.addObserver(obs);
		harvester.start(defaultTestParams(OAIVerb.LIST_RECORDS), h);

		final Supplier<HarvestNotification> first = () -> {
			return AdditionalMatchers.and(
					NotificationMatchers.harvestStarted(),
					NotificationMatchers.withStats(0, 0));
		};
		inOrderVerify(h).onHarvestStart(first.get());
		inOrderVerify(obs).update(eq(harvester), first.get());
		for (long request = 1; request < totalResponses; request++) {
			final Function<Long, HarvestNotification> respRcvd = (r) -> {
				return AdditionalMatchers.and(
						NotificationMatchers.responseReceived(),
						NotificationMatchers.withStats(r, r));
			};
			inOrderVerify(h).onResponseReceived(respRcvd.apply(request));
			inOrderVerify(h).getEventHandler(respRcvd.apply(request));
			inOrderVerify(h).onResponseProcessed(AdditionalMatchers.and(
					NotificationMatchers.responseProcessedSuccessfully(),
					NotificationMatchers.withStats(request, request)));
		}
		final Supplier<HarvestNotification> lastRespRcvd = () -> {
			return AdditionalMatchers.and(
					NotificationMatchers.lastResponseProcessedSuccessfully(),
					NotificationMatchers.withStats(totalResponses,
							totalResponses)) ;
		};
		inOrderVerify(h).onResponseProcessed(lastRespRcvd.get());
		inOrderVerify(obs).update(eq(harvester), lastRespRcvd.get());
		
		final Supplier<HarvestNotification> last = () -> {
			return AdditionalMatchers.and(
					NotificationMatchers.harvestEndedSuccessfully(),
					NotificationMatchers.withStats(totalResponses,
							totalResponses));
		};
		inOrderVerify(h).onHarvestEnd(last.get());
		inOrderVerify(obs).update(eq(harvester), last.get());
	}

	@Test
	public void testListRecords() throws Exception {
		setupWithDefaultListRecordsResponse(true);
		listRecordsTest(2);
	}

	@Test
	public void testListRecordsWithNoFinalResumptionToken() throws Exception {
		setupWithDefaultListRecordsResponse(false);
		listRecordsTest(2);
	}
}
