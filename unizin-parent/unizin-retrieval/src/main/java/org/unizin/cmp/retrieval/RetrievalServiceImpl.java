package org.unizin.cmp.retrieval;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RetrievalServiceImpl extends DefaultComponent implements RetrievalService {

    private static final Logger LOG = LoggerFactory.getLogger(RetrievalServiceImpl.class);

    private final Map<String, RetrieverDescriptor> retrieverDescriptors = new HashMap<>();

    private CloseableHttpClient httpClient;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        httpClient = HttpClients.createDefault();
    }

    @Override
    public void deactivate(ComponentContext context) {
        try {
            httpClient.close();
        } catch (IOException e) {
            LOG.warn("error while closing httpClient", e);
        }
        super.deactivate(context);
    }

    @Override
    public Blob retrieveFileContent(DocumentModel document) {
        // TODO: generalize to other document types/facets
        if (!document.hasFacet("Harvested")) {
            String msg = String.format("document %s does not have 'Harvested' facet",
                                       document.getId());
            throw new RetrievalException(msg);
        }
        String baseUrl = (String) document.getPropertyValue("hrv:sourceRepository");
        RetrieverDescriptor desc = retrieverDescriptors.get(baseUrl);
        if (desc == null) {
            String msg = String.format(
                    "don't know how to retrieve files from %s for document %s",
                    baseUrl, document.getId());
            throw new RetrievalException(msg);
        }
        return desc.instance().retrieveFileContent(httpClient, document);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
                                     ComponentInstance contributor) {
        super.registerContribution(contribution, extensionPoint, contributor);
        RetrieverDescriptor desc = (RetrieverDescriptor) contribution;
        retrieverDescriptors.put(desc.getSourceRepository(), desc);
    }

}
