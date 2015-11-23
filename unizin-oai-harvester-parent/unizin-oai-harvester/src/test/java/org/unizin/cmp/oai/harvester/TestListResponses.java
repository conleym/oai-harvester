package org.unizin.cmp.oai.harvester;

import static org.mockito.Matchers.eq;
import static org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType.HARVEST_ENDED;
import static org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType.RESPONSE_PROCESSED;
import static org.unizin.cmp.oai.harvester.Tests.defaultTestParams;
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

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
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
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestStatistic;
import org.unizin.cmp.oai.harvester.exception.OAIProtocolException;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;
import org.unizin.cmp.oai.mocks.MockHttpClient;
import org.unizin.cmp.oai.mocks.Mocks;
import org.unizin.cmp.oai.mocks.NotificationMatchers;
import org.unizin.cmp.oai.templates.ErrorsTemplate;
import org.unizin.cmp.oai.templates.ListRecordsTemplate;
import org.unizin.cmp.oai.templates.RecordMetadataTemplate;

import freemarker.template.TemplateException;


public final class TestListResponses {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TestListResponses.class);

    public static final long DEFAULT_RESPONSE_COUNT = 2;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private MockHttpClient mockHttpClient;

    private static final ResumptionToken FIRST_TOKEN =
            new ResumptionToken("the first token", 2L, 1L, null);
    private static final ResumptionToken LAST_TOKEN =
            new ResumptionToken("", DEFAULT_RESPONSE_COUNT,
                    DEFAULT_RESPONSE_COUNT, null);

    private static final List<ResumptionToken> RESUMPTION_TOKENS =
            Arrays.asList(FIRST_TOKEN, LAST_TOKEN);

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

    private static void addRecord(final ListRecordsTemplate listRecordsTemplate,
            final String identifier,
            final RecordMetadataTemplate recordMetadataTemplate)
                    throws TemplateException, IOException {
        final Map<String, Object> record = new HashMap<>(2);
        record.put("identifier", identifier);
        record.put("metadata", recordMetadataTemplate.process());
        listRecordsTemplate.addRecord(record);
    }

    @Before
    public void initMockHttpClient() {
        mockHttpClient = new MockHttpClient();
    }

    private Harvester newHarvester() {
        return new Harvester.Builder()
                .withHttpClient(mockHttpClient)
                .build();
    }

    /**
     * Set up with two incomplete lists. First has two records, second has one.
     *
     * @param sendFinalResumptionToken
     *            should we follow the standard and send an empty resumption
     *            token in the last incomplete list? If {@code false}, do what
     *            many repositories actually do, and send no token at all.
     */
    public static void setupWithDefaultListRecordsResponse(
            final boolean sendFinalResumptionToken,
            final MockHttpClient mockClient)
                    throws TemplateException, IOException {
        ListRecordsTemplate listRecordsTemplate = new ListRecordsTemplate()
                .withResumptionToken(toMap(FIRST_TOKEN));
        RecordMetadataTemplate recordMetadataTemplate =
                new RecordMetadataTemplate()
                .addTitle("A Title")
                .addCreator("Some Creator")
                .addCreator("Another Creator");
        addRecord(listRecordsTemplate, "1", recordMetadataTemplate);

        recordMetadataTemplate = new RecordMetadataTemplate()
                .addTitle("Another Title")
                .addTitle("Yet More Title")
                .addDate("2015-10-31")
                .addDate("1900-01-01");
        addRecord(listRecordsTemplate, "2", recordMetadataTemplate);
        String resp = listRecordsTemplate.process();
        mockClient.addResponseFrom(200, "", resp);

        listRecordsTemplate = new ListRecordsTemplate();
        if (sendFinalResumptionToken) {
            listRecordsTemplate.withResumptionToken(toMap(LAST_TOKEN));
        }
        recordMetadataTemplate = new RecordMetadataTemplate()
                .addTitle("Such Title Wow");
        addRecord(listRecordsTemplate, "3", recordMetadataTemplate);
        resp = listRecordsTemplate.process();
        mockClient.addResponseFrom(200, "", resp);
    }

    private void listRecordsTest(final Harvester harvester,
            final long totalResponses,
            final List<ResumptionToken> resumptionTokens)
                    throws TemplateException, IOException {
        final OAIResponseHandler h = Mocks.newResponseHandler();
        final Observer obs = Mockito.mock(Observer.class);
        harvester.addObserver(obs);
        harvester.start(defaultTestParams().build(), h);

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
        setupWithDefaultListRecordsResponse(true, mockHttpClient);
        listRecordsTest(newHarvester(), DEFAULT_RESPONSE_COUNT,
                RESUMPTION_TOKENS);
    }

    /**
     * Tests that the harvester correctly stops when no &lt;resumptionToken&gt;
     * element is found in the response.
     */
    @Test
    public void testListRecordsWithNoFinalResumptionToken() throws Exception {
        setupWithDefaultListRecordsResponse(false, mockHttpClient);
        listRecordsTest(newHarvester(), DEFAULT_RESPONSE_COUNT,
                Arrays.asList(FIRST_TOKEN, new ResumptionToken("")));
    }

    /**
     * Tests that the harvester handles the
     * {@link OAIErrorCode#BAD_RESUMPTION_TOKEN} error correctly.
     */
    @Test
    public void testListRecordsWithBadResumptionToken() throws Exception {
        final ListRecordsTemplate lrt = new ListRecordsTemplate()
                .withResumptionToken(toMap(FIRST_TOKEN));
        final Map<String, Object> record = new HashMap<>();
        record.put("metadata", new RecordMetadataTemplate().process());
        lrt.addRecord(record);
        final String firstResp = lrt.process();
        LOGGER.debug("First response: {}", firstResp);
        mockHttpClient.addResponseFrom(HttpStatus.SC_OK, "", firstResp);
        final List<OAIError> errors = Arrays.asList(new OAIError(
                OAIErrorCode.BAD_RESUMPTION_TOKEN.code(),
                "A helpful message."));
        final String secondResp = ErrorsTemplate.process(errors);
        LOGGER.debug("Second response: {}", secondResp);
        mockHttpClient.addResponseFrom(HttpStatus.SC_OK, "", secondResp);
        exception.expect(OAIProtocolException.class);
        try {
            newHarvester().start(defaultTestParams(OAIVerb.LIST_RECORDS)
                    .build(),
                    Mocks.newResponseHandler());
        } catch (final OAIProtocolException e) {
            Assert.assertEquals(errors, e.getOAIErrors());
            throw e;
        }
    }


    /**
     * Tests that {@code Observers} can stop the harvest via
     * {@link Harvester#stop()}.
     */
    @Test
    public void testStop() throws Exception {
        setupWithDefaultListRecordsResponse(true, mockHttpClient);
        final Observer obs = (o, arg) -> {
            final Harvester h = (Harvester)o;
            final HarvestNotification hn = (HarvestNotification)arg;
            if (hn.getType() == RESPONSE_PROCESSED &&
                    hn.getStat(HarvestStatistic.REQUEST_COUNT) == 1L) {
                h.stop();
            }
        };

        final OAIResponseHandler rh = Mocks.newResponseHandler();
        final Harvester harvester = newHarvester();
        harvester.addObserver(obs);
        final Observer mockObserver = Mockito.mock(Observer.class);
        harvester.addObserver(mockObserver);
        harvester.start(defaultTestParams(OAIVerb.LIST_RECORDS).build(), rh);

        inOrderVerify(rh).onHarvestStart(NotificationMatchers.harvestStarted());
        inOrderVerify(mockObserver).update(eq(harvester),
                NotificationMatchers.harvestStarted());
        inOrderVerify(rh).onResponseReceived(
                NotificationMatchers.responseReceived());
        inOrderVerify(mockObserver).update(eq(harvester),
                NotificationMatchers.responseReceived());
        inOrderVerify(rh).onResponseProcessed(
                NotificationMatchers.responseProcessedSuccessfully());
        inOrderVerify(mockObserver).update(eq(harvester),
                NotificationMatchers.responseProcessedSuccessfully());

        final Supplier<HarvestNotification> lastNotification = () -> {
            return AdditionalMatchers.and(
                    NotificationMatchers.withStats(1, 1),
                    Mocks.matcherFromPredicate(
                            (hn) -> {
                                return hn.getType() == HARVEST_ENDED &&
                                        !hn.isRunning() && hn.isExplicitlyStopped() &&
                                        !hn.hasError();
                            },
                            HarvestNotification.class));
        };

        // Harvest ends. Second incomplete list not retrieved.
        inOrderVerify(rh).onHarvestEnd(lastNotification.get());
        inOrderVerify(mockObserver).update(eq(harvester),
                lastNotification.get());
    }

    /**
     * Tests that the harvest continues even if an observer throws.
     * <p>
     * Observers really shouldn't throw, but we can't be too careful.
     */
    public void testObserverException() throws Exception {
        setupWithDefaultListRecordsResponse(true, mockHttpClient);
        final Harvester harvester = newHarvester();
        // Add a badly-behaved observer.
        harvester.addObserver((o, arg) -> {
            throw new RuntimeException(Mocks.TEST_EXCEPTION_MESSAGE);
        });
        listRecordsTest(harvester, DEFAULT_RESPONSE_COUNT, RESUMPTION_TOKENS);
    }
}
