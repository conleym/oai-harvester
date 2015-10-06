package org.unizin.catalog.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.catalog.OAIConstants;
import org.unizin.catalog.importer.oai.Record;
import org.unizin.catalog.importer.oai.RecordLoader;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.ZipFile;

public class SimpleImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleImporter.class);

    private final String nuxeoUser;
    private final String nuxeoPassword;
    private final String nuxeoApiRoot;
    private final ZipFile inputFile;
    private final XMLInputFactory inputFactory;
    private final XMLOutputFactory outputFactory;
    private final XMLEventFactory eventFactory;
    private final DocumentBuilderFactory documentBuilderFactory;
    private final RecordLoader loader;

    public SimpleImporter(ZipFile zipFile) {
        nuxeoApiRoot = System.getProperty(
                "SimpleImporter.nuxeoApiRoot",
                "http://localhost:8080/nuxeo/site/automation");
        nuxeoUser = System.getProperty("SimpleImporter.nuxeoUser",
                                       "Administrator");
        nuxeoPassword = System.getProperty("SimpleImporter.nuxeoPassword",
                                           "Administrator");
        inputFile = zipFile;
        inputFactory = XMLInputFactory.newInstance();
        outputFactory = XMLOutputFactory.newInstance();
        eventFactory = XMLEventFactory.newInstance();
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        loader = new RecordLoader(nuxeoApiRoot, nuxeoUser, nuxeoPassword);
    }

    public static void main(String[] args) throws IOException {
        if (args == null || args.length != 1) {
            System.out.println("usage: java SimpleImporter harvestsFile.zip");
            System.exit(1);
        }
        String fileName = args[0];
        try (ZipFile zipFile = new ZipFile(fileName)) {
            SimpleImporter importer = new SimpleImporter(zipFile);
            importer.start();
        }
    }

    public void start() {
        LOGGER.info("Starting with target Nuxeo: {}", nuxeoApiRoot);
        inputFile.stream().filter(z1 -> !z1.isDirectory()).forEach(z2 -> {
            try {
                handleListRecords(inputFile.getInputStream(z2));
            } catch (IOException | XMLStreamException |
                    ParserConfigurationException | SAXException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void handleListRecords(InputStream inputStream) throws
            XMLStreamException,
            IOException,
            ParserConfigurationException,
            SAXException {
        XMLEventReader reader = inputFactory.createXMLEventReader(inputStream);
        URI baseUri = null;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (isRequestElement(event)) {
                XMLEvent charsEvent = reader.peek();
                String uriStr = null;
                if (charsEvent.isCharacters()) {
                    try {
                        uriStr = charsEvent.asCharacters().getData();
                        baseUri = new URI(uriStr);
                    } catch (URISyntaxException e) {
                        LOGGER.warn("error parsing request URI '{}'", uriStr);
                    }
                }
            }
            if (isStartOfRecord(event)) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                XMLEventWriter writer = outputFactory.createXMLEventWriter(buffer);
                writer.add(event);
                writer.add(
                        eventFactory.createNamespace(OAIConstants.OAI_NS_URI));
                writer.add(
                        eventFactory.createNamespace("xsi", OAIConstants.XSI_NS_URI));
                do {
                    event = reader.nextEvent();
                    writer.add(event);
                } while (!isEndOfRecord(event));
                writer.close();
                DocumentBuilder documentBuilder =
                        documentBuilderFactory.newDocumentBuilder();
                Document doc = documentBuilder.parse(
                        new ByteArrayInputStream(buffer.toByteArray()));
                Record record = new Record(doc, baseUri);
                loader.load(String.format(
                                    "/default-domain/workspaces/Harvested/%s",
                                    baseUri.getHost()), record);
            }
        }
    }

    private boolean isRequestElement(XMLEvent event) {
        if (!event.isStartElement()) {
            return false;
        }
        return OAIConstants.REQUEST.equals(event.asStartElement().getName());
    }

    private boolean isStartOfRecord(XMLEvent event) {
        if (!event.isStartElement()) {
            return false;
        }
        return OAIConstants.RECORD.equals(event.asStartElement().getName());
    }

    private boolean isEndOfRecord(XMLEvent event) {
        if (!event.isEndElement()) {
            return false;
        }
        return OAIConstants.RECORD.equals(event.asEndElement().getName());
    }

}
