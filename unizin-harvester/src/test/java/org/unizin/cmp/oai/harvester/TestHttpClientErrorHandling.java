package org.unizin.cmp.oai.harvester;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;

public final class TestHttpClientErrorHandling extends HarvesterTestBase {
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	/**
	 * Tests that {@code RuntimeExceptions} thrown by {@code HttpClient} are
	 *  propagated to the caller.
	 */
	@Test
	public void testHttpClientRuntimeExceptions() throws Exception {
		// HttpClient can actually throw this one, so seems reasonable to use.
		mockHttpClient.setRuntimeException(
				new IllegalStateException("Testing!"));
		final Harvester harvester = defaultTestHarvester();
		// Verb doesn't matter here.
		final HarvestParams params = defaultTestParams();
		exception.expect(IllegalStateException.class);
		harvester.start(params, NULL_HANDLER);
	}
	
	/**
	 * Tests that {@code IOExceptions} thrown by {@code HttpClient} are wrapped
	 * in a {@code HarvesterException} and propagated to the caller.
	 */
	@Test
	public void testHttpClientCheckedExceptions() throws Exception {
		mockHttpClient.setCheckedException(new IOException("Testing!"));
		final Harvester harvester = defaultTestHarvester();
		// Verb doesn't matter here.
		final HarvestParams params = defaultTestParams();
		exception.expect(HarvesterException.class);
		try {
			harvester.start(params, NULL_HANDLER);
		} catch (final HarvesterException e) {
			final Throwable cause = e.getCause();
			// Check non-null separately for better error reporting.
			Assert.assertNotNull(cause);
			Assert.assertTrue(cause instanceof IOException);
			throw e;
		}
	}
}
