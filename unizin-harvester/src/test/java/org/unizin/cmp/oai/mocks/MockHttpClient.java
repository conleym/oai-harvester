package org.unizin.cmp.oai.mocks;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;


/**
 * Mock {@link HttpClient} implementation.
 * <p>
 * Yes, we could use mockito for this, but I think this is easier.
 *
 */
//We have to implement the interface, regardless of deprecations.
@SuppressWarnings("deprecation")
public final class MockHttpClient implements HttpClient {

    private List<MockHttpResponse> responses = new ArrayList<>();
    private Iterator<MockHttpResponse> iterator;
    private IOException checkedException;
    private RuntimeException runtimeException;

    public void addResponses(final MockHttpResponse...responses) {
        this.responses.addAll(Arrays.asList(responses));
    }

    public void addResponseFrom(final int statusCode, final String reasonPhrase,
            final String responseBody) throws IOException {
        final StatusLine sl = new BasicStatusLine(HttpVersion.HTTP_1_1,
                statusCode, reasonPhrase);
        final MockHttpResponse response = new MockHttpResponse(sl);
        response.setEntityContent(responseBody);
        addResponses(response);
    }

    public void addResponseFrom(final int statusCode, final String reasonPhrase,
            final InputStream responseContent) throws IOException {
        final StatusLine sl = new BasicStatusLine(HttpVersion.HTTP_1_1,
                statusCode, reasonPhrase);
        final MockHttpResponse response = new MockHttpResponse(sl);
        response.setEntityContent(responseContent);
        addResponses(response);
    }

    public List<MockHttpResponse> getResponses() {
        return this.responses;
    }

    public void setCheckedException(final IOException checkedException) {
        this.checkedException = checkedException;
    }

    public void setRuntimeException(final RuntimeException runtimeException) {
        this.runtimeException = runtimeException;
    }

    @Override
    public HttpResponse execute(final HttpUriRequest request)
            throws IOException, ClientProtocolException {
        if (checkedException != null) {
            throw checkedException;
        }
        if (runtimeException != null) {
            throw runtimeException;
        }
        if (iterator == null) {
            iterator = responses.iterator();
        }
        return iterator.next();
    }

    // None of the other methods below are ever called by the harvester so need
    // no mocking.

    @Override
    public HttpParams getParams() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpResponse execute(HttpUriRequest request, HttpContext context)
            throws IOException, ClientProtocolException {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request)
            throws IOException, ClientProtocolException {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request,
            HttpContext context) throws IOException, ClientProtocolException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T execute(HttpUriRequest request,
            ResponseHandler<? extends T> responseHandler)
                    throws IOException, ClientProtocolException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T execute(HttpUriRequest request,
            ResponseHandler<? extends T> responseHandler, HttpContext context)
                    throws IOException, ClientProtocolException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request,
            ResponseHandler<? extends T> responseHandler)
                    throws IOException, ClientProtocolException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request,
            ResponseHandler<? extends T> responseHandler,
            HttpContext context) throws IOException, ClientProtocolException {
        throw new UnsupportedOperationException();
    }
}
