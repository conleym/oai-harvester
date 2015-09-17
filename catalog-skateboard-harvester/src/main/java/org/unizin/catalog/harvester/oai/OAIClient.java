package org.unizin.catalog.harvester.oai;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.apache.http.util.TextUtils.isEmpty;
import static org.unizin.catalog.OAIConstants.*;

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

    public void listRecords(URI baseURI, OutputStream outputStream) throws
            IOException, XMLStreamException {
        URIBuilder builder = new URIBuilder(baseURI);
        try {
            URI requestURI = builder.addParameter("verb", "ListRecords")
                    .addParameter("metadataPrefix", "oai_dc").build();
            HttpGet get = new HttpGet(requestURI);
            HttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            ResumptionToken resumptionToken;
            if (entity != null) {
                InputStream content = entity.getContent();
                resumptionToken = parseResponse(content, outputStream);
                System.out.println(resumptionToken.toString());
                while (!isEmpty(resumptionToken.token)) {
                    builder.clearParameters();
                    builder.addParameter("verb", "ListRecords");
                    builder.addParameter("resumptionToken",
                                         resumptionToken.token);
                    requestURI = builder.build();
                    get = new HttpGet(requestURI);
                    response = httpClient.execute(get);
                    entity = response.getEntity();
                    content = entity.getContent();
                    resumptionToken = parseResponse(content, outputStream);
                    LOGGER.info(baseURI.toString() + ": " + resumptionToken.toString());
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
        LocalDateTime expirationDate = null;
        int cursor = -1;
        int completeListSize = -1;
        String token = null;
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                StartElement se = event.asStartElement();
                if (RESUMPTION_TOKEN.equals(se.getName())) {
                    if (se.getAttributeByName(CURSOR) != null) {
                        cursor = Integer.parseInt(
                                se.getAttributeByName(CURSOR).getValue());
                    }
                    if (se.getAttributeByName(COMPLETE_LIST_SIZE) != null) {
                        completeListSize = Integer.parseInt(
                                se.getAttributeByName(COMPLETE_LIST_SIZE).getValue());
                    }
                    if (se.getAttributeByName(EXPIRATION_DATE) != null) {
                        String val = se.getAttributeByName(EXPIRATION_DATE).getValue();
                        LocalDateTime dt = LocalDateTime.parse(val,
                                                               DateTimeFormatter.ISO_DATE_TIME);
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
        return new ResumptionToken(expirationDate, completeListSize, cursor, token);
    }
}
