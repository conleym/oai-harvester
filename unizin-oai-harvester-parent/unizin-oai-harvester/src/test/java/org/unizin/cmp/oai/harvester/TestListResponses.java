package org.unizin.cmp.oai.harvester;

import static org.mockito.Matchers.eq;
import static org.unizin.cmp.oai.harvester.ListResponses.DEFAULT_RESPONSE_COUNT;
import static org.unizin.cmp.oai.harvester.ListResponses.FIRST_TOKEN;
import static org.unizin.cmp.oai.harvester.ListResponses.RESUMPTION_TOKENS;
import static org.unizin.cmp.oai.harvester.ListResponses.setupWithDefaultListRecordsResponse;
import static org.unizin.cmp.oai.harvester.ListResponses.toMap;
import static org.unizin.cmp.oai.harvester.Tests.newParams;
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
import org.unizin.cmp.oai.mocks.WireMockUtils;
import org.unizin.cmp.oai.templates.ErrorsTemplate;
import org.unizin.cmp.oai.templates.ListRecordsTemplate;
import org.unizin.cmp.oai.templates.RecordMetadataTemplate;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import freemarker.template.TemplateException;

/**
 * Test list responses and associated resumptionToken and error handling.
 */
public final class TestListResponses {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TestListResponses.class);
    @Rule
    public final WireMockRule wireMock = WireMockUtils.newWireMockRule();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private void listRecordsTest(final Harvester harvester,
            final long totalResponses,
            final List<ResumptionToken> resumptionTokens)
                    throws TemplateException, IOException {
        final OAIResponseHandler h = Mocks.newResponseHandler();
        final Observer obs = Mockito.mock(Observer.class);
        harvester.addObserver(obs);
        harvester.start(newParams().build(), h);

        // Observer and response handler... get harvest started event.
        final Supplier<HarvestNotification> hrvStarted = () -> {
            return AdditionalMatchers.and(
                    NotificationMatchers.harvestStarted(),
                    NotificationMatchers.withStats(0, 0));
        };
        inOrderVerify(h).onHarvestStart(hrvStarted.get());
        inOrderVerify(obs).update(eq(harvester), hrvStarted.get());

        /* ... get each response received & processed notifications w/ the
         * right stats and tokens for all but the last response. */
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
        // ... get appropriate last response received/processed notifications.
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

        // ... get harvest ended event.
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
        listRecordsTest(new Harvester.Builder().build(),
                DEFAULT_RESPONSE_COUNT, RESUMPTION_TOKENS);
    }

    /**
     * Tests that the harvester correctly stops when no &lt;resumptionToken&gt;
     * element is found in the response.
     */
    @Test
    public void testListRecordsWithNoFinalResumptionToken() throws Exception {
        setupWithDefaultListRecordsResponse(false);
        listRecordsTest(new Harvester.Builder().build(),
                DEFAULT_RESPONSE_COUNT, Arrays.asList(FIRST_TOKEN,
                        new ResumptionToken("")));
    }

    /**
     * Tests that the harvester handles the
     * {@link OAIErrorCode#BAD_RESUMPTION_TOKEN} error correctly.
     */
    @Test
    public void testListRecordsWithBadResumptionToken() throws Exception {
        // Build first response. No errors, just a record.
        final ListRecordsTemplate lrt = new ListRecordsTemplate()
                .withResumptionToken(toMap(FIRST_TOKEN));
        final Map<String, Object> record = new HashMap<>();
        record.put("metadata", new RecordMetadataTemplate().process());
        lrt.addRecord(record);
        final String firstResponse = lrt.process();
        LOGGER.debug("First response: {}", firstResponse);

        // Build second response w/ bad resumption token OAI protocol error.
        final List<OAIError> errors = Arrays.asList(new OAIError(
                OAIErrorCode.BAD_RESUMPTION_TOKEN.code(),
                "A helpful message."));
        final String secondResponse = ErrorsTemplate.process(errors);
        LOGGER.debug("Second response: {}", secondResponse);
        final Map<String, String> subsequentResponses = new HashMap<>(1);
        subsequentResponses.put(FIRST_TOKEN.getToken(), secondResponse);
        // WireMock setup with the given responses....
        ListResponses.setupResponses(firstResponse, subsequentResponses);

        exception.expect(OAIProtocolException.class);
        try {
            final HarvestParams params = newParams(
                    OAIVerb.LIST_RECORDS).build();
            new Harvester.Builder().build().start(params,
                    Mocks.newResponseHandler());
        } catch (final OAIProtocolException e) {
            Assert.assertEquals(errors, e.getOAIErrors());
            throw e;
        }
    }

    /**
     * Tests that the harvest continues even if an observer throws.
     * <p>
     * Observers really shouldn't throw, but we can't be too careful.
     * </p>
     */
    public void testObserverException() throws Exception {
        setupWithDefaultListRecordsResponse(true);
        final Harvester harvester = new Harvester.Builder().build();
        // Add a badly-behaved observer.
        harvester.addObserver((o, arg) -> {
            throw new RuntimeException(Mocks.TEST_EXCEPTION_MESSAGE);
        });
        listRecordsTest(harvester, DEFAULT_RESPONSE_COUNT, RESUMPTION_TOKENS);
    }
}
