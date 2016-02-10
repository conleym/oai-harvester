package org.unizin.cmp.oai.harvester.service.client;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
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
    private final ServiceHttpClientWrapper httpClientWrapper;
    private final URI nuxeoURI;
    private final int pageSize;


    private static NuxeoClientException statusException(final int statusCode,
            final String responseBody) {
        return new NuxeoClientException(String.format(
                "Got status %d. Response %s", statusCode, responseBody));
    }

    public NuxeoClient(final ObjectMapper mapper, final HttpClient httpClient,
            final URI nuxeoURI, final String username, final String password,
            final int pageSize) {
        this.objectMapper = mapper;
        final BasicAuthHttpClient basicAuthHttpClient =
                new BasicAuthHttpClient(httpClient, nuxeoURI.getHost(),
                        username, password, false);
        this.httpClientWrapper = new ServiceHttpClientWrapper(LOGGER,
                basicAuthHttpClient,
                Collections.singleton(HttpStatus.SC_OK),
                NuxeoClient::statusException,
                () -> new NuxeoClientException("Got null entity from Nuxeo."),
                e -> new NuxeoClientException(e));
        this.nuxeoURI = nuxeoURI;
        this.pageSize = pageSize;
    }

    private Map<String, Object> execute(final HttpUriRequest request) {
        final HttpResponse response = httpClientWrapper.execute(request);
        final HttpEntity entity = response.getEntity();
        try (final InputStream in = entity.getContent();
                final Reader r = new InputStreamReader(in,
                        httpClientWrapper.contentEncoding(entity))) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> map =
            (Map<String, Object>)objectMapper.readValue(r,
                    Map.class);
            LOGGER.debug("Map for request {} is {}", request, map);
            return map;
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new NuxeoClientException(e);
        }
    }

    public Iterable<Map<String, Object>> repositories() {
        return new QueryIterable(REPOSITORY_NXQL);
    }
}
