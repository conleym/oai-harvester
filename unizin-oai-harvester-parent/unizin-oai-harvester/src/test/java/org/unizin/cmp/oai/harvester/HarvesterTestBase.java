package org.unizin.cmp.oai.harvester;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.mocks.MockHttpClient;


public class HarvesterTestBase {
    protected static final Logger LOGGER = LoggerFactory.getLogger(
            "harvester-tests");

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
