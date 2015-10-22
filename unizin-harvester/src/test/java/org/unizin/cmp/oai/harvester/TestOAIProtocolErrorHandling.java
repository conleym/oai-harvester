package org.unizin.cmp.oai.harvester;

import java.util.Arrays;
import java.util.Collections;
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
import org.unizin.cmp.oai.templates.ErrorsTemplate;


public final class TestOAIProtocolErrorHandling extends HarvesterTestBase {
	@Rule
	public ExpectedException exception = ExpectedException.none();

	private final List<OAIError> defaultErrorList = 
			Collections.unmodifiableList(Arrays.asList(
					new OAIError(OAIErrorCode.BAD_ARGUMENT.code())));

	private void simpleTest(final List<OAIError> errors)
			throws Exception {
		final String errorResponse = ErrorsTemplate.process(errors);
		mockHttpClient.setResponseFrom(HttpStatus.SC_OK, "", errorResponse);
		final Harvester harvester = defaultTestHarvester();
		// Verb doesn't matter here.
		final HarvestParams params = defaultTestParams();
		exception.expect(OAIProtocolException.class);
		try {
			harvester.start(params, NULL_HANDLER);
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
		simpleTest(defaultErrorList);
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
	 */
	@Test
	public void testPriorityOverParseErrors() throws Exception {
		final String errorResponse = ErrorsTemplate.process(defaultErrorList);
		mockHttpClient.setResponseFrom(HttpStatus.SC_OK, "", 
				errorResponse + " some extra content making the XML invalid.");
		final Harvester harvester = defaultTestHarvester();
		// Verb doesn't matter here.
		final HarvestParams params = defaultTestParams();
		exception.expect(OAIProtocolException.class);
		try {
			harvester.start(params, NULL_HANDLER);
		} catch (final OAIProtocolException e) {
			Assert.assertEquals(defaultErrorList, e.getOAIErrors());
			final Throwable[] suppressed = e.getSuppressed();
			Assert.assertEquals(1, suppressed.length);
			Assert.assertTrue(
					suppressed[0] instanceof HarvesterXMLParsingException);
			throw e;
		}
	}
}
