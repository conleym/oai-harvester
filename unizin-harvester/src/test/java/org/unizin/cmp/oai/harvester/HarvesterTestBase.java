package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;
import org.unizin.cmp.oai.mocks.MockHttpClient;
import org.unizin.cmp.oai.mocks.NullOAIResponseHandler;

public class HarvesterTestBase {
	public static final OAIVerb DEFAULT_VERB = OAIVerb.GET_RECORD;
	public static final OAIResponseHandler NULL_HANDLER =
			new NullOAIResponseHandler();
	
	public static final URI TEST_URI;
	static {
		try {
			TEST_URI = new URI("http://test/oai");
		} catch (final URISyntaxException e) {
			throw new ExceptionInInitializerError(e);
		}
	}
	
	public static HarvestParams defaultTestParams() {
		return new HarvestParams(TEST_URI, DEFAULT_VERB);
	}
	
	public static HarvestParams defaultTestParams(final OAIVerb verb) {
		return new HarvestParams(TEST_URI, verb);
	}
	
	protected MockHttpClient mockHttpClient;
	
	@Before
	public void initMockClient() {
		mockHttpClient = new MockHttpClient();
	}
	
	protected Harvester defaultTestHarvester() {
		final Harvester harvester = new Harvester.Builder()
				.withHttpClient(mockHttpClient)
				.build();
		return harvester;
	}
	
	protected HarvesterTestBase() {}
}
