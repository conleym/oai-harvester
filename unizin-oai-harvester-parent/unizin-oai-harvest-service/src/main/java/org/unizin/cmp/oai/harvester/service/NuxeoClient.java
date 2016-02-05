package org.unizin.cmp.oai.harvester.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.harvester.OAIRequestFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


public final class NuxeoClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            NuxeoClient.class);
    private static final String REPOSITORY_NXQL =
            "select * from RemoteRepository";


    public static final class NuxeoClientException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public NuxeoClientException() {
            super();
        }

        public NuxeoClientException(final Throwable cause) {
            super(cause);
        }

        public NuxeoClientException(final String message) {
            super(message);
        }
    }


    private final class QueryIterable implements Iterable<Map<String, Object>> {
        private final String query;
        private int currentPage = 0;
        private int pageCount = 1;

        private QueryIterable(final String query) {
            this.query = query;
        }

        @Override
        public Iterator<Map<String, Object>> iterator() {
            return new Iterator<Map<String, Object>>() {
                @Override
                public boolean hasNext() {
                    return currentPage < pageCount;
                }

                @Override
                public Map<String, Object> next() {
                    final Map<String, String> params = new TreeMap<>();
                    params.put("query", query);
                    params.put("pageSize", String.valueOf(pageSize));
                    params.put("currentPageIndex",
                            String.valueOf(currentPage));
                    final Map<String, Object> map = execute(
                            OAIRequestFactory.get(nuxeoURI, params));
                    pageCount = (Integer)map.get("pageCount");
                    currentPage = (Integer)map.get("currentPageIndex") + 1;
                    return map;
                }
            };
        }
    }


    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI nuxeoURI;
    private final int pageSize;
    private final CredentialsProvider credentialsProvider;


    public NuxeoClient(final ObjectMapper mapper, final HttpClient httpClient,
            final URI nuxeoURI, final String user, final String password,
            final int pageSize) {
        this.objectMapper = mapper;
        this.httpClient = httpClient;
        this.nuxeoURI = nuxeoURI;
        this.pageSize = pageSize;
        this.credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(nuxeoURI.getHost(), AuthScope.ANY_PORT,
                        AuthScope.ANY_REALM, "basic"),
                new UsernamePasswordCredentials(user, password));
    }

    private String contentEncoding(final HttpEntity entity) {
        return Status.headerValue(entity.getContentEncoding())
                .orElse(StandardCharsets.ISO_8859_1.name());
    }

    private Map<String, Object> execute(final HttpUriRequest request) {
        try {
            final HttpContext context = new BasicHttpContext();
            context.setAttribute(HttpClientContext.CREDS_PROVIDER,
                    credentialsProvider);
            LOGGER.trace("Sending request {}", request);
            final HttpResponse response = httpClient.execute(request, context);
            LOGGER.debug("Got response {} to request {}", response, request);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new NuxeoClientException(
                        String.format("Got status %s from Nuxeo.", statusCode));
            }
            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new NuxeoClientException("Got null entity from Nuxeo.");
            }
            try (final InputStream in = entity.getContent();
                    final Reader r = new InputStreamReader(in,
                            contentEncoding(entity))) {
                @SuppressWarnings("unchecked")
                final Map<String, Object> map =
                        (Map<String, Object>)objectMapper.readValue(r,
                                Map.class);
                LOGGER.debug("Map for request {} is {}", request, map);
                return map;
            }
        } catch (final IOException e) {
            throw new NuxeoClientException(e);
        }
    }

    public Iterable<Map<String, Object>> repositories() {
        return new QueryIterable(REPOSITORY_NXQL);
    }
}
