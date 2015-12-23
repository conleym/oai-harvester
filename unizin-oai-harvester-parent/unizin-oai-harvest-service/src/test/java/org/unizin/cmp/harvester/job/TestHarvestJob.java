package org.unizin.cmp.harvester.job;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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


public final class TestHarvestJob {
    @Rule
    public final WireMockRule wireMockRule = Tests.newWireMockRule();

    @Rule
    public final ExpectedException exception = ExpectedException.none();
    private final URI testURI;
    private final DynamoDBTestClient dynamoDBTestClient = new DynamoDBTestClient(
            this.getClass().getSimpleName());


    public TestHarvestJob() throws URISyntaxException {
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

    private HarvestJob.Builder newJobBuilder() {
        return new HarvestJob.Builder(dynamoDBTestClient.mapper);
    }

    private Set<HarvestedOAIRecord> expectedRecords(final String response)
            throws Exception {
        final Set<HarvestedOAIRecord> set = new HashSet<>();
        final JobOAIEventHandler handler = new JobOAIEventHandler(testURI,
                (arg) -> set.add(arg));
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
        return set;
    }

    private void doRun(final String serverResponseBody) throws Exception {
        final Set<HarvestedOAIRecord> expectedRecords = expectedRecords(
                serverResponseBody);
        final HarvestJob job = newJobBuilder()
                .withHarvestParams(new HarvestParams.Builder(testURI,
                        OAIVerb.LIST_RECORDS).build())
                .build();
        stubFor(get(urlMatching(".*"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(serverResponseBody)));
        job.start();
        final List<HarvestedOAIRecord> actualRecords =
                dynamoDBTestClient.scan(HarvestedOAIRecord.class);
        Assert.assertEquals(expectedRecords.size(), actualRecords.size());

        final Map<String, HarvestedOAIRecord> map = new HashMap<>();
        for (final HarvestedOAIRecord expectedRecord : expectedRecords) {
            map.put(expectedRecord.getIdentifier(), expectedRecord);
        }
        for (final HarvestedOAIRecord actualRecord : actualRecords) {
            final HarvestedOAIRecord expectedRecord = map.get(
                    actualRecord.getIdentifier());
            /*
             * Because the harvested timestamp is set on the record when it's
             * created, we have no idea what the actual value should be. So
             * we'll just accept whatever comes out as correct.
             */
            expectedRecord.setHarvestedTimestamp(
                    actualRecord.getHarvestedTimestamp());
            Assert.assertEquals(expectedRecord,
                    actualRecord);
        }
    }

    @Test
    public void testJobRun() throws Exception {
        doRun(Tests.OAI_LIST_RECORDS_RESPONSE);

        final List<String> updatedRecords = new ArrayList<String>(
                Tests.RAW_TEST_RECORDS);
        updatedRecords.remove(2);
        final InputStream in = this.getClass().getResourceAsStream(
                "/oai-records/record-3a.xml");
        final String updatedRecord3 = new String(ByteStreams.toByteArray(in),
                StandardCharsets.UTF_8);
        updatedRecords.add(updatedRecord3);
        final String updatedResponse = Tests.listRecordsResponse(updatedRecords);
        doRun(updatedResponse);
    }

    @Test
    public void testDuplicates() throws Exception {
        final List<String> listWithDuplicates = new ArrayList<String>(2);
        final String test0 = Tests.RAW_TEST_RECORDS.get(0);
        listWithDuplicates.add(test0);
        listWithDuplicates.add(test0);
        final String response = Tests.listRecordsResponse(listWithDuplicates);
        doRun(response);
    }
}
