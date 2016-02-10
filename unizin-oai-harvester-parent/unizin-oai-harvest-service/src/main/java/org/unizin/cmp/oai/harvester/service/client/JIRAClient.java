package org.unizin.cmp.oai.harvester.service.client;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JIRAClient {

    public static final class JIRAClientException extends RuntimeException {
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
    private final ServiceHttpClientWrapper httpClientWrapper;
    private final ObjectMapper objectMapper;


    private static JIRAClientException statusException(final int statusCode,
            final String responseBody) {
        return new JIRAClientException(String.format(
                "Got status %d. Response %s", statusCode, responseBody));
    }

    public JIRAClient(final URI endpoint, final HttpClient httpClient,
            final String username, final String password,
            final ObjectMapper objectMapper) {
        this.endpoint = endpoint;
        final BasicAuthHttpClient basicAuthHttpClient =
                new BasicAuthHttpClient(httpClient, endpoint.getHost(),
                        username, password);
        this.httpClientWrapper = new ServiceHttpClientWrapper(LOGGER,
                basicAuthHttpClient,
                Collections.singleton(HttpStatus.SC_CREATED),
                JIRAClient::statusException,
                () -> new JIRAClientException("Got null entity from JIRA."),
                e -> new JIRAClientException(e));
        this.objectMapper = objectMapper;
    }

    private HttpUriRequest createRequest(final Map<String, Object> issue) {
        try {
            final HttpPost post = new HttpPost(endpoint);
            final String body = objectMapper.writeValueAsString(issue);
            LOGGER.debug("Post body: {}", body);
            final BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(new ByteArrayInputStream(body.getBytes(
                    StandardCharsets.UTF_8)));
            entity.setContentEncoding(StandardCharsets.UTF_8.name());
            entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
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
        httpClientWrapper.execute(createRequest(issue));
    }
}
