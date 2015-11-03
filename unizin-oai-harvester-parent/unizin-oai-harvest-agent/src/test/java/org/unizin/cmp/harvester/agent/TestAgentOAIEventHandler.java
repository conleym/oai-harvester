package org.unizin.cmp.harvester.agent;

import static org.unizin.cmp.harvester.agent.HarvestedOAIRecord.CHECKSUM_ATTRIB;
import static org.unizin.cmp.harvester.agent.HarvestedOAIRecord.DATESTAMP_ATTRIB;
import static org.unizin.cmp.harvester.agent.HarvestedOAIRecord.SETS_ATTRIB;
import static org.unizin.cmp.harvester.agent.HarvestedOAIRecord.STATUS_ATTRIB;
import static org.unizin.cmp.harvester.agent.HarvestedOAIRecord.XML_ATTRIB;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.XMLEvent;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.unizin.cmp.oai.OAI2Constants;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.harvester.HarvestParams;
import org.unizin.cmp.oai.harvester.Harvester;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;


public final class TestAgentOAIEventHandler {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final Configuration configuration = new Configuration(
            Configuration.getVersion());
    private final List<String> records = new ArrayList<>(3);
    private final List<byte[]> checksums = new ArrayList<>(3);
    private final String listRecords;

    public TestAgentOAIEventHandler() throws Exception {
        configuration.setTemplateLoader(new ClassTemplateLoader(
                this.getClass(), "/"));
        final MessageDigest digest = AgentOAIResponseHandler.digest();
        for (final Integer i : Arrays.asList(1, 2, 3)) {
            final String filename = "/record-" + i + ".xml";
            final InputStream in = this.getClass()
                    .getResourceAsStream(filename);
            // Run the expected input through StAX to eliminate any newline weirdness.
            final XMLEventReader r = XMLInputFactory.newFactory().createXMLEventReader(in);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final XMLEventWriter w = XMLOutputFactory.newFactory().createXMLEventWriter(baos);
            while (r.hasNext()) {
               final XMLEvent e = r.nextEvent();
               if (e.isStartDocument() || e.isEndDocument()) {
                   continue;
               }
               w.add(e);
            }
            r.close();
            w.close();
            final byte[] bytes = baos.toByteArray();
            final String str = new String(bytes, StandardCharsets.UTF_8);
            records.add(new String(bytes, StandardCharsets.UTF_8));
            digest.update(str.getBytes(StandardCharsets.UTF_8));
            checksums.add(digest.digest());
        }
        final Template template = configuration.getTemplate("oai-list-records.xml");
        final StringWriter sw = new StringWriter();
        final Map<String, Object> dataModel = new HashMap<>(1);
        dataModel.put("records", records);
        template.process(dataModel, sw);
        this.listRecords = sw.toString();
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
        final HttpClient httpClient = Mockito.mock(HttpClient.class);
        final HttpResponse response = new BasicHttpResponse(
                new BasicStatusLine(HttpVersion.HTTP_1_0,
                        HttpStatus.SC_OK, ""));
        final HttpEntity entity = Mockito.mock(HttpEntity.class);
        final InputStream resp = new ByteArrayInputStream(listRecords.getBytes(StandardCharsets.UTF_8));
        Mockito.doReturn(resp).when(entity).getContent();
        response.setEntity(entity);
        Mockito.doReturn(response).when(httpClient).execute(Matchers.any());
        final Harvester harvester = new Harvester.Builder()
                .withHttpClient(httpClient)
                .build();
        final URI uri = new URI("http://example.oai.com/");
        final HarvestParams p = new HarvestParams(uri, OAIVerb.LIST_RECORDS);
        final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue =
                new ArrayBlockingQueue<>(10);
        harvester.start(p, new AgentOAIResponseHandler(uri,
                harvestedRecordQueue));
        Assert.assertEquals(3, harvestedRecordQueue.size());

        final Map<String, Map<String, Object>> expectedValues = new HashMap<>();
        Map<String, Object> expectedValue = new HashMap<>();
        expectedValue.put(STATUS_ATTRIB, OAI2Constants.DELETED_STATUS);
        expectedValue.put(DATESTAMP_ATTRIB, "2015-11-02");
        expectedValue.put(XML_ATTRIB, records.get(0));
        expectedValue.put(CHECKSUM_ATTRIB, checksums.get(0));
        expectedValue.put(SETS_ATTRIB, Collections.emptySet());
        addExpectedValuesForIdentifier("1", expectedValue, expectedValues);

        expectedValue = new HashMap<>();
        expectedValue.put(DATESTAMP_ATTRIB, "2014-01-10");
        expectedValue.put(XML_ATTRIB, records.get(1));
        expectedValue.put(CHECKSUM_ATTRIB, checksums.get(1));
        expectedValue.put(SETS_ATTRIB, new HashSet<>(Arrays.asList("set1", "set2")));
        addExpectedValuesForIdentifier("2", expectedValue, expectedValues);

        expectedValue = new HashMap<>();
        expectedValue.put(DATESTAMP_ATTRIB, "2010-10-10");
        expectedValue.put(XML_ATTRIB, records.get(2));
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
            equals((byte[])expectedValue.get(CHECKSUM_ATTRIB),
                   record.getChecksum());
            Assert.assertEquals(expectedValue.get(XML_ATTRIB),
                    record.getXml());
        }
    }
}
