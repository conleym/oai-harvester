package org.unizin.cmp.oai.harvester;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.mocks.Mocks;

public final class TestHttpClientErrorHandling extends HarvesterTestBase {	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	/**
	 * Tests that {@code RuntimeExceptions} thrown by {@code HttpClient} are
	 * propagated to the caller.
	 */
	@Test
	public void testHttpClientRuntimeExceptions() throws Exception {
		mockHttpClient.setRuntimeException(
				new NullPointerException(Mocks.TEST_EXCEPTION_MESSAGE));
		final Harvester harvester = defaultTestHarvester();
		// Verb doesn't matter here.
		final HarvestParams params = defaultTestParams();
		exception.expect(NullPointerException.class);
		exception.expectMessage(Mocks.TEST_EXCEPTION_MESSAGE);
		harvester.start(params, Mocks.newResponseHandler());
	}
	
	/**
	 * Tests that {@code IOExceptions} thrown by {@code HttpClient} are wrapped
	 * in {@code UncheckedIOExceptions} and propagated to the caller.
	 */
	@Test
	public void testHttpClientCheckedExceptions() throws Exception {
		mockHttpClient.setCheckedException(new IOException(
				Mocks.TEST_EXCEPTION_MESSAGE));
		final Harvester harvester = defaultTestHarvester();
		// Verb doesn't matter here.
		final HarvestParams params = defaultTestParams();
		exception.expect(UncheckedIOException.class);
		try {
			harvester.start(params, Mocks.newResponseHandler());
		} catch (final UncheckedIOException e) {
			final Throwable cause = e.getCause();
			Mocks.assertTestException(cause, IOException.class);
			throw e;
		}
	}
}
