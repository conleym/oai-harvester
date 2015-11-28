package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.util.Map;

import org.apache.http.client.methods.HttpUriRequest;

/**
 * A factory that produces HTTP GET requests.
 * <p>
 * This is default {@link OAIRequestFactory} implementation used by the
 * {@link Harvester} if none is specified.
 * </p>
 * <p>
 * This factory generates HTTP GET requests using
 * {@link OAIRequestFactory#get(URI, Map)}.
 * </p>
 */
public final class GetOAIRequestFactory implements OAIRequestFactory {
    private static final GetOAIRequestFactory INSTANCE =
            new GetOAIRequestFactory();

    public static GetOAIRequestFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public HttpUriRequest createRequest(final URI baseURI,
            final Map<String, String> parameters) {
        return OAIRequestFactory.get(baseURI, parameters);
    }

    /** Enforce singleton for stateless class. */
    private GetOAIRequestFactory() { }
}
