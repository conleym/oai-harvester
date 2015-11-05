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

import javax.xml.stream.XMLEventReader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.OAIXMLUtils;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.HarvestParams;
import org.unizin.cmp.oai.harvester.response.AbstractOAIResponseHandler;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;


public final class TestHarvestAgent {
    @Rule
    public final WireMockRule wireMockRule = Tests.newWireMockRule();

    @Rule
    public final ExpectedException exception = ExpectedException.none();
    private final URI testURI;
    private final DynamoDBTestClient dynamoDBTestClient = new DynamoDBTestClient(
            this.getClass().getSimpleName());


    public TestHarvestAgent() throws URISyntaxException {
        testURI = new URI(Tests.MOCK_OAI_BASE_URI);
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
        final AgentOAIEventHandler handler = new AgentOAIEventHandler(testURI);
        final List<HarvestedOAIRecord> list = new ArrayList<>();
        handler.addObserver((o, arg) -> {
            list.add((HarvestedOAIRecord)arg);
        });
        final OAIResponseHandler h = new AbstractOAIResponseHandler() {
            @Override
            public OAIEventHandler getEventHandler(HarvestNotification notification) {
                return handler;
            }
        };
        final InputStream in = new ByteArrayInputStream(
                Tests.OAI_LIST_RECORDS_RESPONSE.getBytes(
                        StandardCharsets.UTF_8));
        final XMLEventReader reader = OAIXMLUtils.newInputFactory()
                .createXMLEventReader(in);
        while (reader.hasNext()) {
            h.getEventHandler(null).onEvent(reader.nextEvent());
        }
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
