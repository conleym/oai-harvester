package org.unizin.cmp.oai.harvester.job;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.unizin.cmp.oai.harvester.job.HarvestedOAIRecord.CHECKSUM_ATTRIB;
import static org.unizin.cmp.oai.harvester.job.HarvestedOAIRecord.DATESTAMP_ATTRIB;
import static org.unizin.cmp.oai.harvester.job.HarvestedOAIRecord.SETS_ATTRIB;
import static org.unizin.cmp.oai.harvester.job.HarvestedOAIRecord.STATUS_ATTRIB;
import static org.unizin.cmp.oai.harvester.job.HarvestedOAIRecord.XML_ATTRIB;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.OAI2Constants;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.harvester.HarvestParams;
import org.unizin.cmp.oai.harvester.Harvester;

import com.github.tomakehurst.wiremock.junit.WireMockRule;


public final class TestJobOAIEventHandler {
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    @Rule
    public final WireMockRule wireMock = Tests.newWireMockRule();

    private final List<byte[]> checksums = new ArrayList<>();

    public TestJobOAIEventHandler() throws Exception {
        final MessageDigest digest = HarvestJob.digest();
        for (final String record : Tests.EXPECTED_TEST_RECORDS) {
            digest.update(record.getBytes(StandardCharsets.UTF_8));
            checksums.add(digest.digest());
        }
    }

    /**
     * No, {@link java.util.Arrays} has no stream for {@code byte[]}.
     *
     * @param bytes
     *            an array of {@code byte}.
     * @return a list of {@code Byte} with corresponding values.
     */
    private static List<Byte> toList(final byte[] bytes) {
        final List<Byte> list = new ArrayList<>(bytes.length);
        for (final byte b : bytes) {
            final Byte bb = b;
            list.add(bb);
        }
        return list;
    }

    private static void equals(final byte[] expected, final byte[] actual) {
        // Use lists to make exception messages intelligible.
        Assert.assertEquals(toList(expected), toList(actual));
    }

    private static void addExpectedValuesForIdentifier(final String identifier,
            final Map<String, Object> expectedValue,
            final Map<String, Map<String, Object>> expectedValues) {
        expectedValue.put(HarvestedOAIRecord.OAI_ID_ATTRIB, identifier);
        expectedValues.put(identifier, expectedValue);
    }

    @Test
    public void testHandler() throws Exception {
        stubFor(get(urlMatching(".*"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(Tests.OAI_LIST_RECORDS_RESPONSE)));
        final Harvester harvester = new Harvester.Builder().build();
        final URI uri = new URI(Tests.MOCK_OAI_BASE_URI);
        final HarvestParams p = new HarvestParams.Builder(uri,
                OAIVerb.LIST_RECORDS).build();
        final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue =
                new ArrayBlockingQueue<>(Tests.TEST_RECORD_COUNT);
        harvester.start(p, new JobOAIResponseHandler(uri,
                new BlockingQueueWrapper<>(harvestedRecordQueue,
                        Duration.ofMillis(0), Duration.ofMillis(0))));

        Assert.assertEquals(Tests.TEST_RECORD_COUNT,
                harvestedRecordQueue.size());

        final Map<String, Map<String, Object>> expectedValues = new HashMap<>();
        Map<String, Object> expectedValue = new HashMap<>();
        expectedValue.put(STATUS_ATTRIB, OAI2Constants.DELETED_STATUS);
        expectedValue.put(DATESTAMP_ATTRIB, "2015-11-02");
        expectedValue.put(XML_ATTRIB, Tests.EXPECTED_TEST_RECORDS.get(0));
        expectedValue.put(CHECKSUM_ATTRIB, checksums.get(0));
        expectedValue.put(SETS_ATTRIB, Collections.emptySet());
        addExpectedValuesForIdentifier("1", expectedValue, expectedValues);

        expectedValue = new HashMap<>();
        expectedValue.put(DATESTAMP_ATTRIB, "2014-01-10");
        expectedValue.put(XML_ATTRIB, Tests.EXPECTED_TEST_RECORDS.get(1));
        expectedValue.put(CHECKSUM_ATTRIB, checksums.get(1));
        expectedValue.put(SETS_ATTRIB, new HashSet<>(Arrays.asList("set1",
                "set2")));
        addExpectedValuesForIdentifier("2", expectedValue, expectedValues);

        expectedValue = new HashMap<>();
        expectedValue.put(DATESTAMP_ATTRIB, "2010-10-10");
        expectedValue.put(XML_ATTRIB, Tests.EXPECTED_TEST_RECORDS.get(2));
        expectedValue.put(CHECKSUM_ATTRIB, checksums.get(2));
        expectedValue.put(SETS_ATTRIB, new HashSet<>(Arrays.asList("set3")));
        addExpectedValuesForIdentifier("3", expectedValue, expectedValues);

        for (final HarvestedOAIRecord record : harvestedRecordQueue) {
            final String identifier = record.getIdentifier();
            expectedValue = expectedValues.get(identifier);
            Assert.assertEquals(record.getBaseURL(), uri.toString());
            Assert.assertEquals(expectedValue.get(STATUS_ATTRIB),
                    record.getStatus());
            Assert.assertEquals(expectedValue.get(DATESTAMP_ATTRIB),
                    record.getDatestamp());
            Assert.assertEquals(expectedValue.get(XML_ATTRIB),
                    Tests.decompress(record.getXml()));
            equals((byte[])expectedValue.get(CHECKSUM_ATTRIB),
                    record.getChecksum());
        }
    }
}
