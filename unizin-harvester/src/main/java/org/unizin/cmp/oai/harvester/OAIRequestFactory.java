package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;


/**
 * Implementations build {@link HttpUriRequest HttpUriRequests} to pass to
 * {@link HttpClient#execute(HttpUriRequest)} during harvesting.
 * <p>
 * Custom instances could provide, e.g., special request headers or make
 * requests via different HTTP methods. For example, the <a href=
 * "http://www.openarchives.org/OAI/openarchivesprotocol.html#HTTPRequestFormat">
 * OAI-PMH specification</a> allows requests to be sent either as GETs or POSTs.
 * <p>
 * Before writing your own implementation, consider looking into the
 * capabilities of {@code HttpClient}, in particular noting that
 * {@link org.apache.http.impl.client.HttpClientBuilder#setDefaultHeaders(java.util.Collection)}
 * exists and that it's implemented by making use of
 * {@link org.apache.http.HttpRequestInterceptor HttpRequestInterceptors}
 */
public interface OAIRequestFactory {
	/**
	 * Create an HTTP POST request from the given {@code URI} and parameters.
	 * <p>
	 * Note that any query string included in the base {@code URI} will be sent
	 * with the request.
	 * 
	 * @param baseURI the {@code URI} to which the request will be sent.
	 * @param parameters the form parameters to send.
	 * @return an HTTP POST request with the given {@code URI} and parameters.
	 */
	public static HttpUriRequest post(final URI baseURI, 
			final Map<String, String> parameters) {
		final Iterable<? extends NameValuePair> nvps =
				parameters.entrySet().stream().map(e -> 
					new BasicNameValuePair(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
		final HttpPost post = new HttpPost(baseURI);
		post.setEntity(new UrlEncodedFormEntity(nvps));
		return post;		
	}
	
	
	/**
	 * Create an HTTP GET request from the given {@code URI} and parameters.
	 * <p>
	 * Note particularly that any query string in the base {@code URI} is 
	 * preserved and the supplied parameters appended.
	 * 
	 * @param baseURI the {@code URI}.
	 * @param parameters the parameters to add to the {@code baseURI}'s
	 *  query string.
	 * @return an HTTP GET request for the given {@code URI} with the given 
	 * parameters appended to the query string.
	 */
	public static HttpUriRequest get(final URI baseURI, 
			final Map<String, String> parameters) {
		final URIBuilder uriBuilder = new URIBuilder(baseURI);
		parameters.entrySet().stream().forEach(e ->
				uriBuilder.addParameter(e.getKey(), e.getValue()));
		try {
			final HttpGet get = new HttpGet(uriBuilder.build());
			return get;
		} catch (final URISyntaxException e) {
			throw new HarvesterException("Invalid URI syntax for request.", e);
		}
	}


	/**
	 * Create an {@code HttpUriRequest} from the given {@code URI} and 
	 * parameters.
	 * @param baseURI the base {@code URI}.
	 * @param parameters the parameters to send with the request.
	 * @return an {@code HttpUriRequest} with the given base {@code URI} and 
	 * parameters.
	 */
	HttpUriRequest createRequest(URI baseURI,
			Map<String, String> parameters);
}
