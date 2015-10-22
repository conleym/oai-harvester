package org.unizin.cmp.oai.harvester;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.OAIError;
import org.unizin.cmp.oai.OAIErrorCode;
import org.unizin.cmp.oai.harvester.exception.HarvesterXMLParsingException;
import org.unizin.cmp.oai.harvester.exception.OAIProtocolException;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;
import org.unizin.cmp.oai.mocks.Mocks;
import org.unizin.cmp.oai.templates.ErrorsTemplate;

import freemarker.template.TemplateException;


public final class TestOAIProtocolErrorHandling extends HarvesterTestBase {
	@Rule
	public ExpectedException exception = ExpectedException.none();

	private void setupWithDefaultError() throws TemplateException, IOException {
		final String arbitraryValidOAIResponse = ErrorsTemplate.process();
		final InputStream stream = Mocks.fromString(arbitraryValidOAIResponse);
		mockHttpClient.setResponseFrom(HttpStatus.SC_OK, "", stream);
	}
	
	private Throwable checkSingleSuppressedException(final Throwable t) {
		final Throwable[] suppressed = t.getSuppressed();
		Assert.assertEquals(1, suppressed.length);
		return suppressed[0];
	}
	
	private void simpleTest(final List<OAIError> errors)
			throws Exception {
		final String errorResponse = ErrorsTemplate.process(errors);
		mockHttpClient.setResponseFrom(HttpStatus.SC_OK, "", errorResponse);
		final Harvester harvester = defaultTestHarvester();
		// Verb doesn't matter here.
		final HarvestParams params = defaultTestParams();
		exception.expect(OAIProtocolException.class);
		try {
			harvester.start(params, Mocks.newResponseHandler());
		} catch (final OAIProtocolException e) {
			Assert.assertEquals(errors, e.getOAIErrors());
			throw e;
		}
	}

	/**
	 * Tests that an error response with a single &lt;error&gt; element is
	 * processed correctly.
	 */
	@Test
	public void testSingleError() throws Exception {
		simpleTest(ErrorsTemplate.defaultErrorList());
	}
	
	/**
	 * Tests that an error response with multiple &lt;error&gt; elements is
	 * processed correctly.
	 */
	@Test
	public void testMultipleErrors() throws Exception {
		List<OAIError> errors = Arrays.asList(
				new OAIError(OAIErrorCode.BAD_RESUMPTION_TOKEN.code(),
						"Some message about the resumption token"),
				new OAIError(OAIErrorCode.BAD_ARGUMENT.code()),
				new OAIError(OAIErrorCode.CANNOT_DISSEMINATE_FORMAT.code(),
						"Your format is bad and you should feel bad"));
		simpleTest(errors);
	}

	/**
	 * Tests that XML parsing exceptions that occur while processing an error 
	 * response are added as suppressed exceptions to an appropriate
	 * {@code OAIProtocolException}.
	 * <p>
	 * Somewhat obviously, only errors that are processed before the XML parse
	 * error occurred can be reported.
	 */
	@Test
	public void testPriorityOverParseErrors() throws Exception {
		final String errorResponse = ErrorsTemplate.process(
				ErrorsTemplate.defaultErrorList());
		mockHttpClient.setResponseFrom(HttpStatus.SC_OK, "", 
				errorResponse + " some extra content making the XML invalid.");
		final Harvester harvester = defaultTestHarvester();
		// Verb doesn't matter here.
		final HarvestParams params = defaultTestParams();
		exception.expect(OAIProtocolException.class);
		try {
			harvester.start(params, Mocks.newResponseHandler());
		} catch (final OAIProtocolException e) {
			Assert.assertEquals(ErrorsTemplate.defaultErrorList(),
					e.getOAIErrors());
			final Throwable suppressed = checkSingleSuppressedException(e);
			Assert.assertTrue(
					suppressed instanceof HarvesterXMLParsingException);
			throw e;
		}
	}
	
	/**
	 * Tests that, if a stream containing an error response from the server
	 * throws an {@link IOException} when closed, that this exception is added
	 * as a suppressed exception to the {@code OAIProtocolException}.
	 */
	@Test
	public void testPriorityOverStreamClosingErrors() throws Exception {
		final String arbitraryValidOAIResponse = ErrorsTemplate.process();
		final InputStream stream = Mocks.fromString(arbitraryValidOAIResponse);
		mockHttpClient.setResponseFrom(HttpStatus.SC_OK, "", 
				Mocks.throwsWhenClosed(stream));
		final Harvester harvester = defaultTestHarvester();
		final HarvestParams params = defaultTestParams();
		exception.expect(OAIProtocolException.class);
		try {
			harvester.start(params, Mocks.newResponseHandler());
		} catch (final OAIProtocolException e) {
			Assert.assertEquals(ErrorsTemplate.defaultErrorList(),
					e.getOAIErrors());
			final Throwable suppressed = checkSingleSuppressedException(e);
			Mocks.assertTestException(suppressed, IOException.class);
			throw e;
		}
	}
	
	@Test
	public void testPriorityOverResponseHandlerErrors() throws Exception {
		setupWithDefaultError();
		final OAIResponseHandler h = Mocks.newResponseHandler();
		doThrow(new IllegalArgumentException(Mocks.TEST_EXCEPTION_MESSAGE))
			.when(h).onHarvestEnd(any());
		exception.expect(OAIProtocolException.class);
		try {
			defaultTestHarvester().start(defaultTestParams(), h);
		} catch (final OAIProtocolException e) {
			final Throwable suppressed = checkSingleSuppressedException(e);
			Mocks.assertTestException(suppressed,
					IllegalArgumentException.class);
			throw e;
		}
	}
	
	@Test
	public void testOAIHandlerLifecycleCallsAreMade() throws Exception {
		setupWithDefaultError();
		final OAIResponseHandler h = Mocks.newResponseHandler();
		final Harvester harvester = defaultTestHarvester();
		exception.expect(OAIProtocolException.class);
		harvester.start(defaultTestParams(), h);
		// Each of these should be called _exactly_ once.
		verify(h).onHarvestStart(any());
		verify(h).onResponseReceived(any());
		verify(h).onResponseProcessed(any());
		verify(h).onHarvestEnd(any());
		verify(h).getEventHandler(any());
	}
}
