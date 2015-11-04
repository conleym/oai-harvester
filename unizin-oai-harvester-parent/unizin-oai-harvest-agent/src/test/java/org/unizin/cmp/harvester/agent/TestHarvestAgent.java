package org.unizin.cmp.harvester.agent;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLEventReader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.OAIXMLUtils;
import org.unizin.cmp.oai.harvester.HarvestParams;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;


public final class TestHarvestAgent {
    private static final String MOCK_OAI_BASE_URI =
            String.format("http://0.0.0.0:%d/oai", Tests.WIREMOCK_PORT);

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(Tests.WIREMOCK_PORT);

    @Rule
    public final ExpectedException exception = ExpectedException.none();
    private final URI testURI;
    private final DynamoDBTestClient dynamoDBTestClient = new DynamoDBTestClient(
            this.getClass().getSimpleName());


    public TestHarvestAgent() throws URISyntaxException {
        testURI = new URI(MOCK_OAI_BASE_URI);
    }

    @Before
    public void beforeEachTest() {
        try {
            dynamoDBTestClient.dropTable();
        } catch (final ResourceNotFoundException e) {
            /*
             * Don't care. Just want to guard against preexisting table screwing
             * things up.
             */
        }
        dynamoDBTestClient.createTable(HarvestedOAIRecord.class);
        Assert.assertEquals(0,
                dynamoDBTestClient.countItems(HarvestedOAIRecord.class));
    }

    private HarvestAgent.Builder newAgentBuilder() {
        return new HarvestAgent.Builder(dynamoDBTestClient.mapper);
    }

    private List<HarvestedOAIRecord> expectedRecords() throws Exception {
        // TODO: this should be easier to do.
        final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue =
                new ArrayBlockingQueue<>(Tests.TEST_RECORDS.size());
        final AgentOAIResponseHandler h = new AgentOAIResponseHandler(testURI,
                harvestedRecordQueue, new Timeout(0, TimeUnit.SECONDS));
        final InputStream in = new ByteArrayInputStream(
                Tests.OAI_LIST_RECORDS_RESPONSE.getBytes(
                        StandardCharsets.UTF_8));
        final XMLEventReader reader = OAIXMLUtils.newInputFactory()
                .createXMLEventReader(in);
        while (reader.hasNext()) {
            h.getEventHandler(null).onEvent(reader.nextEvent());
        }
        final List<HarvestedOAIRecord> list = new ArrayList<>();
        harvestedRecordQueue.drainTo(list);
        return list;
    }

    @Test
    public void testAgentRun() throws Exception {
        final HarvestAgent agent = newAgentBuilder().build();
        final HarvestParams[] params = {
                new HarvestParams(testURI, OAIVerb.LIST_RECORDS)
        };
        stubFor(get(urlMatching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(Tests.OAI_LIST_RECORDS_RESPONSE)));
        agent.addHarvests(params);
        agent.start();
        Assert.assertEquals(Tests.TEST_RECORDS.size(),
                dynamoDBTestClient.countItems(HarvestedOAIRecord.class));
        final List<HarvestedOAIRecord> scan = dynamoDBTestClient.scan(
                HarvestedOAIRecord.class);
        final Map<String, HarvestedOAIRecord> map = new HashMap<>();
        for (final HarvestedOAIRecord expectedRecord : expectedRecords()) {
            map.put(expectedRecord.getIdentifier(), expectedRecord);
        }
        for (final HarvestedOAIRecord record : scan) {
            Assert.assertEquals(map.get(record.getIdentifier()), record);
        }
    }
}
