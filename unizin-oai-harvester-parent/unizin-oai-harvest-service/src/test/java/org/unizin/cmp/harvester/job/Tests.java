package org.unizin.cmp.harvester.job;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.ByteStreams;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/** Test utilities. */
public final class Tests {
    public static final String DEFAULT_DYNAMO_PORT = "8000";
    public static final String DYNAMO_PORT = System.getProperty(
            "dynamodb.port", DEFAULT_DYNAMO_PORT);

    public static final int DEFAULT_WIREMOCK_PORT = 9000;
    public static final int WIREMOCK_PORT = Integer.parseInt(
            System.getProperty("wiremock.port",
                    String.valueOf(DEFAULT_WIREMOCK_PORT)));

    public static final String MOCK_OAI_BASE_URI =
            String.format("http://0.0.0.0:%d/oai", Tests.WIREMOCK_PORT);

    private static final Template OAI_LIST_RECORDS_TEMPLATE;
    static {
        final Configuration config = new Configuration(Configuration.getVersion());
        config.setTemplateLoader(new ClassTemplateLoader(
                Tests.class, "/oai-response-templates"));
        try {
            OAI_LIST_RECORDS_TEMPLATE = config.getTemplate(
                    "oai-list-records.ftl.xml");
        } catch (final IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static final List<String> TEST_RECORDS;
    static {
        final int count = 3;
        final List<String> responses = new ArrayList<>(count);
        try {
            for (int i = 1; i <= count; i++) {
                responses.add(testOAIRecord(i));
            }
            TEST_RECORDS = Collections.unmodifiableList(
                    responses);
        } catch (final XMLStreamException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static final String OAI_LIST_RECORDS_RESPONSE;
    static {
        OAI_LIST_RECORDS_RESPONSE = listRecordsResponse(TEST_RECORDS);
    }

    public static String listRecordsResponse(final List<String> records) {
        try {
            final StringWriter sw = new StringWriter();
            final Map<String, Object> dataModel = new HashMap<>(1);
            dataModel.put("records", records);
            OAI_LIST_RECORDS_TEMPLATE.process(dataModel, sw);
            return sw.toString();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch (final TemplateException e) {
            throw new RuntimeException(e);
        }
    }

    private static String testOAIRecord(int i) throws XMLStreamException {
        final InputStream in = Tests.class.getResourceAsStream(
                "/oai-records/record-" + i + ".xml");
        if (in == null) {
            throw new IllegalArgumentException(String.format(
                    "Nonexistant response file requested: %d.", i));
        }
        /*
         * Run the expected input through StAX to eliminate any newline
         * weirdness.
         */
        final XMLEventReader r = XMLInputFactory.newFactory()
                .createXMLEventReader(in);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final XMLEventWriter w = XMLOutputFactory.newFactory()
                .createXMLEventWriter(baos);
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
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String decompress(final byte[] bytes) throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try (final GZIPInputStream in = new GZIPInputStream(bais)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteStreams.copy(in, baos);
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    public static WireMockRule newWireMockRule() {
        return new WireMockRule(WIREMOCK_PORT);
    }

    /** No instances allowed. */
    private Tests() { }
}
