package org.unizin.cmp.oai.harvester.job;

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
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;

import org.unizin.cmp.oai.OAI2Constants;
import org.unizin.cmp.oai.OAIXMLUtils;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.ByteStreams;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/** Test utilities. */
public final class Tests {
    public static final String DEFAULT_DYNAMO_PORT = "8000";
    public static final String DYNAMO_PORT;
    static {
        // For some reason, System.getProperty("prop", "default")
        // returns "" when we run in IntelliJ, so I guess we need
        // to be explicit.
        String prop = System.getProperty("dynamodb.port");
        if (prop == null || "".equals(prop.trim())) {
            prop = DEFAULT_DYNAMO_PORT;
        }
        DYNAMO_PORT = prop;
    }

    public static final int DEFAULT_WIREMOCK_PORT = 9000;
    public static final int WIREMOCK_PORT;
    static {
        // See DYNAMO_PORT for explanation.
        String prop = System.getProperty("wiremock.port");
        if (prop == null || "".equals(prop.trim())) {
            prop = String.valueOf(DEFAULT_WIREMOCK_PORT);
        }
        WIREMOCK_PORT = Integer.parseInt(prop);
    }

    public static final String MOCK_OAI_BASE_URI =
            String.format("http://0.0.0.0:%d/oai", Tests.WIREMOCK_PORT);

    private static final Template OAI_LIST_RECORDS_TEMPLATE;
    static {
        final Configuration config = new Configuration(
                Configuration.getVersion());
        config.setTemplateLoader(new ClassTemplateLoader(
                Tests.class, "/oai-response-templates"));
        try {
            OAI_LIST_RECORDS_TEMPLATE = config.getTemplate(
                    "oai-list-records.ftl.xml");
        } catch (final IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static final int TEST_RECORD_COUNT = 3;

    public static final List<String> EXPECTED_TEST_RECORDS;
    static {
        final List<String> records = new ArrayList<>(TEST_RECORD_COUNT);
        try {
            for (int i = 1; i <= TEST_RECORD_COUNT; i++) {
                records.add(testOAIRecord(i));
            }
            EXPECTED_TEST_RECORDS = Collections.unmodifiableList(
                    records);
        } catch (final XMLStreamException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    public static final List<String> RAW_TEST_RECORDS;
    static {
        try {
            final List<String> records = new ArrayList<>(TEST_RECORD_COUNT);
            for (int i = 1; i <= TEST_RECORD_COUNT; i++) {
                records.add(rawOAIRecord(i));
            }
            RAW_TEST_RECORDS = Collections.unmodifiableList(records);
        } catch (final IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }


    public static final String OAI_LIST_RECORDS_RESPONSE;
    static {
        OAI_LIST_RECORDS_RESPONSE = listRecordsResponse(RAW_TEST_RECORDS);
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

    public static String readRecord(final InputStream in)
            throws XMLStreamException {
        /*
         * Run the expected input through StAX to eliminate any newline
         * weirdness and to add appropriate namespaces.
         */
        final XMLEventFactory ef = XMLEventFactory.newFactory();
        final XMLEventReader r = XMLInputFactory.newFactory()
                .createXMLEventReader(in);
        final Set<Namespace> ns = Collections.singleton(
                ef.createNamespace(OAI2Constants.OAI_2_NS_URI));
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final XMLEventWriter w = OAIXMLUtils.createEventWriter(
                XMLOutputFactory.newFactory(), baos);
        while (r.hasNext()) {
            final XMLEvent e = r.nextEvent();
            if (e.isStartDocument() || e.isEndDocument()) {
                continue;
            }
            if (e.isStartElement() && OAI2Constants.RECORD.getLocalPart()
                    .equals(e.asStartElement().getName().getLocalPart())) {
                w.add(ef.createStartElement(OAI2Constants.RECORD, null,
                        ns.iterator()));

            } else {
                w.add(e);
            }
        }
        r.close();
        w.close();
        final byte[] bytes = baos.toByteArray();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String testRecordFilename(final int i) {
        return "/oai-records/record-" + i + ".xml";
    }

    private static InputStream testRecordStream(final int i) {
        final String filename = testRecordFilename(i);
        final InputStream in = Tests.class.getResourceAsStream(filename);
        if (in == null) {
            throw new IllegalArgumentException(String.format(
                    "Nonexistant response file requested: %s.", filename));
        }
        return in;
    }

    private static String rawOAIRecord(final int i) throws IOException {
        return new String(ByteStreams.toByteArray(testRecordStream(i)),
                StandardCharsets.UTF_8);
    }

    private static String testOAIRecord(final int i) throws XMLStreamException {
        return readRecord(testRecordStream(i));
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
