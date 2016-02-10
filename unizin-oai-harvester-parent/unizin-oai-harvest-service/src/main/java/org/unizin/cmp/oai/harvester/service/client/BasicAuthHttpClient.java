package org.unizin.cmp.oai.harvester.service.client;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

/**
 * Simple wrapper around {@link HttpClient} that handles HTTP basic
 * authentication for a specific host.
 */
final class BasicAuthHttpClient {
    private final HttpClient httpClient;
    private final CredentialsProvider credentialsProvider
        = new BasicCredentialsProvider();

    BasicAuthHttpClient(final HttpClient httpClient,
            final String authScopeHost, final String username,
            final String password) {
        this.httpClient = httpClient;
        credentialsProvider.setCredentials(
                new AuthScope(authScopeHost, AuthScope.ANY_PORT,
                        AuthScope.ANY_REALM, "basic"),
                new UsernamePasswordCredentials(username, password));
    }

    HttpResponse execute(final HttpUriRequest request) throws IOException {
        final HttpContext context = new BasicHttpContext();
        context.setAttribute(HttpClientContext.CREDS_PROVIDER,
                credentialsProvider);
        return httpClient.execute(request, context);
    }
}
