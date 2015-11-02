package org.unizin.catalog.importer.oai;

import org.nuxeo.ecm.automation.client.Constants;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class RecordLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordLoader.class);

    private final HttpAutomationClient client;

    private Document parentDoc;

    public RecordLoader(String nuxeoApiRoot, String nuxeoUser,
                        String nuxeoPassword) {
        client = new HttpAutomationClient(nuxeoApiRoot);
        client.setBasicAuth(nuxeoUser, nuxeoPassword);
    }

    public void load(String parentPath, Record record) throws IOException {
        Session session = client.getSession();
        if (parentDoc == null ||
            !(parentDoc.getPath()).equals(parentPath)) {
            parentDoc = ensureExists(session, parentPath);
        }
        String title = record.getTitle().orElse("Untitled");
        Document doc = new Document(title, "File");
        doc.set("dc:title", title);
        doc.set("hrv:title", asPropertyList(record, "title"));
        doc.set("hrv:creator", asPropertyList(record, "creator"));
        doc.set("hrv:subject", asPropertyList(record, "subject"));
        doc.set("hrv:description", asPropertyList(record, "description"));
        doc.set("hrv:publisher", asPropertyList(record, "publisher"));
        doc.set("hrv:contributor", asPropertyList(record, "contributor"));
        doc.set("hrv:date", asPropertyList(record, "date"));
        doc.set("hrv:type", asPropertyList(record, "type"));
        doc.set("hrv:format", asPropertyList(record, "format"));
        doc.set("hrv:identifier", asPropertyList(record, "identifier"));
        doc.set("hrv:source", asPropertyList(record, "source"));
        doc.set("hrv:language", asPropertyList(record, "language"));
        doc.set("hrv:relation", asPropertyList(record, "relation"));
        doc.set("hrv:coverage", asPropertyList(record, "coverage"));
        doc.set("hrv:rights", asPropertyList(record, "rights"));
        doc.set("hrv:sourceRepository", String.valueOf(record.getBaseUri()));
        doc.set("hrv:oaiIdentifier", record.getOaiIdentifier().orElse("NONE"));

        try {
            doc = (Document) session.newRequest("Document.Create")
                    .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                    .setInput(parentPath)
                    .set("type", doc.getType())
                    .set("name", doc.getTitle())
                    .set("properties", doc).execute();
            LOGGER.info("{}", doc.getTitle());
        } catch (RemoteException e) {
            LOGGER.warn("Failed to create document", e);
        }
    }

    private Document ensureExists(Session session, String parentPath) throws
            IOException {
        try {
            return (Document) session.newRequest("Document.Fetch")
                    .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                    .set("value", parentPath).execute();
        } catch (RemoteException e) {
            int lastSep = parentPath.lastIndexOf('/');
            String firstPart = parentPath.substring(0, lastSep);
            String lastPart = parentPath.substring(lastSep + 1);
            Document parentDoc = (Document) session.newRequest("Document.Fetch")
                    .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                    .set("value", firstPart).execute();
            Document childDoc = new Document(lastPart, "Folder");
            childDoc.set("dc:title", lastPart);
            return (Document) session.newRequest("Document.Create")
                    .setHeader(Constants.HEADER_NX_SCHEMAS, "*")
                    .set("type", "Folder")
                    .setInput(parentDoc)
                    .set("name", lastPart)
                    .set("properties", childDoc).execute();
        }
    }

    private String asPropertyList(Record record, String dcLocalName) {
        List<String> results = record.getDCElements(dcLocalName)
                .stream()
                .map(item -> item.replace(",", "\\,"))
                .map(item -> item.replace("\n", "\\\n"))
                .collect(Collectors.toList());
        return String.join(",", results);
    }
}
