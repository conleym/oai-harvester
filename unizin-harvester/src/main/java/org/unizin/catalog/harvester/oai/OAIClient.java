package org.unizin.catalog.harvester.oai;

import static org.apache.http.util.TextUtils.isEmpty;
import static org.unizin.cmp.oai.OAI2Constants.RESUMPTION_TOKEN;
import static org.unizin.cmp.oai.OAI2Constants.RT_COMPLETE_LIST_SIZE_ATTR;
import static org.unizin.cmp.oai.OAI2Constants.RT_CURSOR_ATTR;
import static org.unizin.cmp.oai.OAI2Constants.RT_EXPIRATION_DATE_ATTR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.ResumptionToken;

public class OAIClient {

    private final static Logger LOGGER = LoggerFactory.getLogger(OAIClient.class);

    private final HttpClient httpClient;
    private final XMLInputFactory xmlInputFactory;
    private final XMLOutputFactory xmlOutputFactory;

    public OAIClient() {
        httpClient = HttpClients.createDefault();
        xmlInputFactory = XMLInputFactory.newInstance();
        xmlOutputFactory = XMLOutputFactory.newInstance();
    }

    public OAIClient(HttpClient client) {
        httpClient = client;
        xmlInputFactory = XMLInputFactory.newInstance();
        xmlOutputFactory = XMLOutputFactory.newInstance();
    }

    public void identify(URI baseURI, OutputStream outputStream) throws
            IOException {
        URIBuilder builder = new URIBuilder(baseURI);
        try {
            URI requestURI = builder.addParameter("verb", "Identify").build();
            HttpGet get = new HttpGet(requestURI);
            HttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try {
                    entity.writeTo(outputStream);
                } finally {
                    EntityUtils.consume(entity);
                }
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void listRecords(URI baseURI, String setName, File outputDir) throws
            IOException, XMLStreamException {
        URIBuilder builder = new URIBuilder(baseURI);
        try {
            int count = 0;
            URI requestURI = builder.addParameter("verb", "ListRecords")
                    .addParameter("metadataPrefix", "oai_dc")
                    .addParameter("set", setName).build();
            HttpGet get = new HttpGet(requestURI);
            HttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            ResumptionToken resumptionToken;
            if (entity != null) {
                InputStream content = entity.getContent();
                String filename = String.format("file%d.xml", ++count);
                try (OutputStream outputStream = new FileOutputStream(new File(outputDir, filename))) {
                    resumptionToken = parseResponse(content, outputStream);
                }
                System.out.println(resumptionToken.toString());
                while (!isEmpty(resumptionToken.getToken())) {
                    builder.clearParameters();
                    builder.addParameter("verb", "ListRecords");
                    builder.addParameter("resumptionToken",
                                         resumptionToken.getToken());
                    requestURI = builder.build();
                    get = new HttpGet(requestURI);
                    response = httpClient.execute(get);
                    entity = response.getEntity();
                    content = entity.getContent();
                    filename = String.format("file%d.xml", ++count);
                    try (OutputStream outputStream = new FileOutputStream(new File(outputDir, filename))) {
                        resumptionToken = parseResponse(content, outputStream);

                    }
                    LOGGER.info(baseURI.toString() + ": " +
                                resumptionToken.toString());
                }

            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private ResumptionToken parseResponse(InputStream content,
                                          OutputStream outputStream) throws
            XMLStreamException {
        XMLEventReader reader = xmlInputFactory.createXMLEventReader(content);
        XMLEventWriter writer = xmlOutputFactory.createXMLEventWriter(
                outputStream);
        Instant expirationDate = null;
        int cursor = -1;
        int completeListSize = -1;
        String token = null;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                if (RESUMPTION_TOKEN.equals(se.getName())) {
                    if (se.getAttributeByName(RT_CURSOR_ATTR) != null) {
                        cursor = Integer.parseInt(
                                se.getAttributeByName(RT_CURSOR_ATTR).getValue());
                    }
                    if (se.getAttributeByName(RT_COMPLETE_LIST_SIZE_ATTR) != null) {
                        completeListSize = Integer.parseInt(
                                se.getAttributeByName(RT_COMPLETE_LIST_SIZE_ATTR).getValue());
                    }
                    if (se.getAttributeByName(RT_EXPIRATION_DATE_ATTR) != null) {
                        String val = se.getAttributeByName(RT_EXPIRATION_DATE_ATTR).getValue();
                        Instant dt = Instant.parse(val);
                        expirationDate = dt;
                    }
                    XMLEvent nextEvent = reader.peek();
                    if (!nextEvent.isCharacters()) {
                        System.out.println("expected characters!");
                    } else {
                        Characters c = nextEvent.asCharacters();
                        token = c.getData();
                    }
                }
            }
            writer.add(event);
            writer.flush();
        }
        return new ResumptionToken(token, (long)completeListSize, (long)cursor,
                expirationDate);
    }
}
