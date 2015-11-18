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

import org.apache.http.HttpStatus;
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
import com.google.common.io.ByteStreams;


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

    private List<HarvestedOAIRecord> expectedRecords(final String response)
            throws Exception {
        final List<HarvestedOAIRecord> list = new ArrayList<>();
        final AgentOAIEventHandler handler = new AgentOAIEventHandler(testURI,
                (arg) -> list.add(arg));
        final OAIResponseHandler h = new AbstractOAIResponseHandler() {
            @Override
            public OAIEventHandler getEventHandler(
                    final HarvestNotification notification) {
                return handler;
            }
        };
        final InputStream in = new ByteArrayInputStream(response.getBytes(
                StandardCharsets.UTF_8));
        final XMLEventReader reader = OAIXMLUtils.newInputFactory()
                .createXMLEventReader(in);
        while (reader.hasNext()) {
            h.getEventHandler(null).onEvent(reader.nextEvent());
        }
        return list;
    }

    private void doRun(final String serverResponseBody) throws Exception {
        final List<HarvestedOAIRecord> expectedRecords = expectedRecords(
                serverResponseBody);
        final HarvestAgent agent = newAgentBuilder()
                .withHarvestParams(new HarvestParams(testURI, OAIVerb.LIST_RECORDS))
                .build();
        stubFor(get(urlMatching(".*"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(serverResponseBody)));
        agent.start();
        final List<HarvestedOAIRecord> actualRecords =
                dynamoDBTestClient.scan(HarvestedOAIRecord.class);
        Assert.assertEquals(expectedRecords.size(), actualRecords.size());

        final Map<String, HarvestedOAIRecord> map = new HashMap<>();
        for (final HarvestedOAIRecord expectedRecord : expectedRecords) {
            map.put(expectedRecord.getIdentifier(), expectedRecord);
        }
        for (final HarvestedOAIRecord actualRecord : actualRecords) {
            Assert.assertEquals(map.get(actualRecord.getIdentifier()),
                    actualRecord);
        }
    }

    @Test
    public void testAgentRun() throws Exception {
        doRun(Tests.OAI_LIST_RECORDS_RESPONSE);

        final List<String> updatedRecords = new ArrayList<String>(
                Tests.TEST_RECORDS);
        updatedRecords.remove(2);
        final InputStream in = this.getClass().getResourceAsStream(
                "/oai-records/record-3a.xml");
        final String updatedRecord3 = new String(ByteStreams.toByteArray(in),
                StandardCharsets.UTF_8);
        updatedRecords.add(updatedRecord3);
        final String updatedResponse = Tests.listRecordsResponse(updatedRecords);
        doRun(updatedResponse);
    }
}
