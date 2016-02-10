package org.unizin.cmp.oai.harvester.service.client;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;

/**
 * Simple wrapper around {@link HttpClient} that handles HTTP basic
 * authentication for a specific host.
 */
final class BasicAuthHttpClient {
    private final HttpClient httpClient;
    private final CredentialsProvider credentialsProvider
        = new BasicCredentialsProvider();
    private final AuthCache authCache = new BasicAuthCache();

    BasicAuthHttpClient(final HttpClient httpClient,
            final String authScopeHost, final String username,
            final String password, final boolean preemptivelyAuthenticate) {
        this.httpClient = httpClient;
        credentialsProvider.setCredentials(
                new AuthScope(authScopeHost, AuthScope.ANY_PORT,
                        AuthScope.ANY_REALM, "basic"),
                new UsernamePasswordCredentials(username, password));
        // See https://hc.apache.org/httpcomponents-client-4.5.x/tutorial/html/authentication.html#d5e717
        if (preemptivelyAuthenticate) {
            final HttpHost host = new HttpHost(authScopeHost, -1, "https");
            authCache.put(host, new BasicScheme());
        }
    }

    HttpResponse execute(final HttpUriRequest request) throws IOException {
        final HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(credentialsProvider);
        context.setAuthCache(authCache);
        return httpClient.execute(request, context);
    }
}
