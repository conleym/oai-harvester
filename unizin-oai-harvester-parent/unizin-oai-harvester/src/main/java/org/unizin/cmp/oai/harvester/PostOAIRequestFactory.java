package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.util.Map;

import org.apache.http.client.methods.HttpUriRequest;

/**
 * A factory that produces HTTP POST requests.
 * <p>
 * This factory generates HTTP POST requests using
 * {@link OAIRequestFactory#post(URI, Map)}.
 * </p>
 */
public final class PostOAIRequestFactory implements OAIRequestFactory {
    private static final PostOAIRequestFactory INSTANCE =
            new PostOAIRequestFactory();

    public static PostOAIRequestFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public HttpUriRequest createRequest(final URI baseURI,
            final Map<String, String> parameters) {
        return OAIRequestFactory.post(baseURI, parameters);
    }

    /** Enforce singleton for stateless class. */
    private PostOAIRequestFactory() { }
}
