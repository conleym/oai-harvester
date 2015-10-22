package org.unizin.cmp.oai.harvester;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;
import org.unizin.cmp.oai.mocks.Mocks;

public final class TestHttpClientErrorHandling extends HarvesterTestBase {
	private static final String MESSAGE = "Testing!";
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	/**
	 * Tests that {@code RuntimeExceptions} thrown by {@code HttpClient} are
	 * propagated to the caller.
	 */
	@Test
	public void testHttpClientRuntimeExceptions() throws Exception {
		mockHttpClient.setRuntimeException(
				new NullPointerException(MESSAGE));
		final Harvester harvester = defaultTestHarvester();
		// Verb doesn't matter here.
		final HarvestParams params = defaultTestParams();
		exception.expect(NullPointerException.class);
		exception.expectMessage(MESSAGE);
		harvester.start(params, Mocks.newResponseHandler());
	}
	
	/**
	 * Tests that {@code IOExceptions} thrown by {@code HttpClient} are wrapped
	 * in a {@code HarvesterException} and propagated to the caller.
	 */
	@Test
	public void testHttpClientCheckedExceptions() throws Exception {
		mockHttpClient.setCheckedException(new IOException(MESSAGE));
		final Harvester harvester = defaultTestHarvester();
		// Verb doesn't matter here.
		final HarvestParams params = defaultTestParams();
		exception.expect(HarvesterException.class);
		try {
			harvester.start(params, Mocks.newResponseHandler());
		} catch (final HarvesterException e) {
			final Throwable cause = e.getCause();
			// Check non-null separately for better error reporting.
			Assert.assertNotNull(cause);
			Assert.assertTrue(cause instanceof IOException);
			Assert.assertEquals(MESSAGE, cause.getMessage());
			throw e;
		}
	}
}
