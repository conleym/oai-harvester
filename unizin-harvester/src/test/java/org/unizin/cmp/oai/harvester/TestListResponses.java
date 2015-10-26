package org.unizin.cmp.oai.harvester;

import static org.mockito.Matchers.eq;
import static org.unizin.cmp.oai.mocks.Mocks.inOrderVerify;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.OAIError;
import org.unizin.cmp.oai.OAIErrorCode;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.ResumptionToken;
import org.unizin.cmp.oai.harvester.exception.OAIProtocolException;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;
import org.unizin.cmp.oai.mocks.Mocks;
import org.unizin.cmp.oai.mocks.NotificationMatchers;
import org.unizin.cmp.oai.templates.ErrorsTemplate;
import org.unizin.cmp.oai.templates.ListRecordsTemplate;
import org.unizin.cmp.oai.templates.RecordMetadataTemplate;

import freemarker.template.TemplateException;


public final class TestListResponses extends HarvesterTestBase {
	private static final Logger LOGGER = 
			LoggerFactory.getLogger(TestListResponses.class);

	@Rule
	public ExpectedException exception = ExpectedException.none();

	private final ResumptionToken firstToken = 
			new ResumptionToken("the first token", 2L, 1L, null);
	private final ResumptionToken lastToken = 
			new ResumptionToken("", 2L, 2L, null);

	private static Map<String, Object> toMap(final ResumptionToken token) {
		final Map<String, Object> m = new HashMap<>();
		m.put("token", token.getToken());
		if (token.getCursor().isPresent()) {
			m.put("cursor", token.getCursor().get());
		}
		if (token.getCompleteListSize().isPresent()) {
			m.put("completeListSize", 
					token.getCompleteListSize().get());
		}
		if (token.getExpirationDate().isPresent()) {
			m.put("expirationDate", token.getExpirationDate().get());
		}
		return m;
	}

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
					throws TemplateException, IOException {
		final Map<String, Object> record = new HashMap<>();

		ListRecordsTemplate listRecordsTemplate = new ListRecordsTemplate()
				.withResumptionToken(toMap(firstToken));
		RecordMetadataTemplate recordMetadataTemplate = 
				new RecordMetadataTemplate()
				.addTitle("A Title")
				.addCreator("Some Creator")
				.addCreator("Another Creator");
		record.put("metadata", recordMetadataTemplate.process());
		listRecordsTemplate.addRecord(record);
		recordMetadataTemplate = new RecordMetadataTemplate()
				.addTitle("Another Title")
				.addTitle("Yet More Title")
				.addDate("2015-10-31")
				.addDate("1900-01-01");
		record.clear();
		record.put("metadata", recordMetadataTemplate.process());
		listRecordsTemplate.addRecord(record);
		String resp = listRecordsTemplate.process();
		LOGGER.debug("First response is {}", resp);
		mockHttpClient.addResponseFrom(200, "", resp);

		listRecordsTemplate = new ListRecordsTemplate();
		if (sendFinalResumptionToken) { 
			listRecordsTemplate.withResumptionToken(toMap(lastToken));
		}
		recordMetadataTemplate = new RecordMetadataTemplate()
				.addTitle("Such Title Wow");
		record.clear();
		record.put("metadata", recordMetadataTemplate.process());
		listRecordsTemplate.addRecord(record);
		resp = listRecordsTemplate.process();
		LOGGER.debug("Second response is {}", resp);
		mockHttpClient.addResponseFrom(200, "", resp);
	}

	private void listRecordsTest(final long totalResponses, 
			final List<ResumptionToken> resumptionTokens)
					throws TemplateException, IOException {
		final Harvester harvester = defaultTestHarvester();
		final OAIResponseHandler h = Mocks.newResponseHandler();
		final Observer obs = Mockito.mock(Observer.class);
		harvester.addObserver(obs);
		harvester.start(defaultTestParams(OAIVerb.LIST_RECORDS), h);

		final Supplier<HarvestNotification> hrvStarted = () -> {
			return AdditionalMatchers.and(
					NotificationMatchers.harvestStarted(),
					NotificationMatchers.withStats(0, 0));
		};
		inOrderVerify(h).onHarvestStart(hrvStarted.get());
		inOrderVerify(obs).update(eq(harvester), hrvStarted.get());
		ResumptionToken previousToken = null;
		final Iterator<ResumptionToken> tokenIterator = 
				resumptionTokens.iterator();
		for (long request = 1; request < totalResponses; request++) {
			final ResumptionToken pt = previousToken;
			final Function<Long, HarvestNotification> respRcvd = (r) -> {
				return AdditionalMatchers.and(
						NotificationMatchers.responseReceived(),
						AdditionalMatchers.and(
								NotificationMatchers.withStats(r, r),
								NotificationMatchers.withToken(pt)));
			};
			inOrderVerify(obs).update(eq(harvester), respRcvd.apply(request));
			inOrderVerify(h).onResponseReceived(respRcvd.apply(request));
			inOrderVerify(h).getEventHandler(respRcvd.apply(request));

			previousToken = tokenIterator.next();
			final ResumptionToken nt = previousToken;
			final Function<Long, HarvestNotification> respProcd = (r) -> {
				return AdditionalMatchers.and(
						NotificationMatchers.responseProcessedSuccessfully(),
						AdditionalMatchers.and(
								NotificationMatchers.withStats(r, r),
								NotificationMatchers.withToken(nt)));
			};
			inOrderVerify(obs).update(eq(harvester), respProcd.apply(request));
			inOrderVerify(h).onResponseProcessed(respProcd.apply(request));
		}
		final ResumptionToken tok = tokenIterator.next();
		final Supplier<HarvestNotification> lastRespRcvd = () -> {
			return AdditionalMatchers.and(
					NotificationMatchers.lastResponseProcessedSuccessfully(),
					AdditionalMatchers.and(
							NotificationMatchers.withStats(totalResponses,
									totalResponses),
							NotificationMatchers.withToken(tok)));
		};
		inOrderVerify(h).onResponseProcessed(lastRespRcvd.get());
		inOrderVerify(obs).update(eq(harvester), lastRespRcvd.get());

		final Supplier<HarvestNotification> hrvEnded = () -> {
			return AdditionalMatchers.and(
					NotificationMatchers.harvestEndedSuccessfully(),
					AdditionalMatchers.and(
							NotificationMatchers.withStats(totalResponses,
									totalResponses),
							NotificationMatchers.withToken(tok)));
		};
		inOrderVerify(h).onHarvestEnd(hrvEnded.get());
		inOrderVerify(obs).update(eq(harvester), hrvEnded.get());
	}

	/**
	 * Tests that the harvester correctly stops when an empty
	 * &lt;resumptionToken&gt; element is found in the response.
	 */
	@Test
	public void testListRecords() throws Exception {
		setupWithDefaultListRecordsResponse(true);
		listRecordsTest(2, Arrays.asList(firstToken, lastToken));
	}

	/**
	 * Tests that the harvester correctly stops when no &lt;resumptionToken&gt;
	 * element is found in the response.
	 */
	@Test
	public void testListRecordsWithNoFinalResumptionToken() throws Exception {
		setupWithDefaultListRecordsResponse(false);
		listRecordsTest(2, Arrays.asList(firstToken, new ResumptionToken("")));
	}

	/**
	 * Tests that the harvester handles the
	 * {@link OAIErrorCode#BAD_RESUMPTION_TOKEN} error correctly.
	 */
	@Test
	public void testListRecordsWithBadResumptionToken() throws Exception {
		final ListRecordsTemplate lrt = new ListRecordsTemplate()
				.withResumptionToken(toMap(firstToken));
		final Map<String, Object> record = new HashMap<>();
		record.put("metadata", new RecordMetadataTemplate().process());
		lrt.addRecord(record);
		final String firstResp = lrt.process();
		LOGGER.debug("First response: {}", firstResp);
		mockHttpClient.addResponseFrom(200, "", firstResp);
		final List<OAIError> errors = Arrays.asList(new OAIError(
				OAIErrorCode.BAD_RESUMPTION_TOKEN.code(),
				"A helpful message."));
		final String secondResp = ErrorsTemplate.process(errors);
		LOGGER.debug("Second response: {}", secondResp);
		mockHttpClient.addResponseFrom(200, "", secondResp);
		final Harvester harvester = defaultTestHarvester();
		final OAIResponseHandler h = Mocks.newResponseHandler();
		exception.expect(OAIProtocolException.class);
		try {
			harvester.start(defaultTestParams(OAIVerb.LIST_RECORDS), h);
		} catch (final OAIProtocolException e) {
			Assert.assertEquals(errors, e.getOAIErrors());
			throw e;
		}
	}
}
