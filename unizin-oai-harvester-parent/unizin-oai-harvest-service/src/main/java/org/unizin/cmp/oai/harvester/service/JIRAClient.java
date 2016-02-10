package org.unizin.cmp.oai.harvester.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JIRAClient {

    public final class JIRAClientException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public JIRAClientException(final String message) {
            super(message);
        }

        public JIRAClientException(final Throwable cause) {
            super(cause);
        }
    }


    private static final Logger LOGGER = LoggerFactory.getLogger(
            JIRAClient.class);

    private final URI endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public JIRAClient(final URI endpoint, final HttpClient httpClient,
            final ObjectMapper objectMapper) {
        this.endpoint = endpoint;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    private HttpUriRequest createRequest(final Map<String, Object> issue) {
        try {
            final HttpPost post = new HttpPost(endpoint);
            final String body = objectMapper.writeValueAsString(issue);
            final BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(new ByteArrayInputStream(body.getBytes(
                    StandardCharsets.UTF_8)));
            entity.setContentEncoding(StandardCharsets.UTF_8.name());
            post.setEntity(entity);
            return post;
        } catch (final JsonProcessingException e) {
            throw new JIRAClientException(e);
        }
    }

    /**
     * Create a new issue.
     *
     * @param issue
     *            a map containing the issue parameters to be sent to JIRA.
     * @throws JIRAClientException
     *             if there's an error creating the issue.
     */
    public void createIssue(final Map<String, Object> issue) {
        final HttpUriRequest request = createRequest(issue);
        LOGGER.trace("Posting {}", request);
        try {
            final HttpResponse response = httpClient.execute(request);
            LOGGER.debug("Got response {} for request {}", response, request);
            final int status = response.getStatusLine().getStatusCode();
            // TODO figure out actual acceptable status codes.
            if (status != HttpStatus.SC_CREATED) {
                throw new JIRAClientException(String.format("Got status %d",
                        status));
            }
            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new JIRAClientException("Got null entity from JIRA.");
            }
        } catch (final IOException e) {
            throw new JIRAClientException(e);
        }
    }
}
