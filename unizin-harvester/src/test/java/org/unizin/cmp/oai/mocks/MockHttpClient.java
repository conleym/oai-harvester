package org.unizin.cmp.oai.mocks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;


//We have to implement the interface, regardless of deprecations.
@SuppressWarnings("deprecation")
public final class MockHttpClient implements HttpClient {

	private HttpResponse response;
	private IOException checkedException;
	private RuntimeException runtimeException;

	public void setResponse(final HttpResponse response) {
		this.response = response;
	}

	public void setResponseFrom(final int statusCode, final String reasonPhrase, 
			final String responseBody) {
		final StatusLine sl = new BasicStatusLine(HttpVersion.HTTP_1_1,
				statusCode, reasonPhrase);
		final byte[] responseBodyBytes = responseBody.getBytes(
				StandardCharsets.UTF_8);
		final HttpEntity entity = EntityBuilder.create()
				.setStream(new ByteArrayInputStream(responseBodyBytes))
				.setContentEncoding(StandardCharsets.UTF_8.toString())
				.build();
		this.response = new BasicHttpResponse(sl);
		response.setEntity(entity);
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
		return response;
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
