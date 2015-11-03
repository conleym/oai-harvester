package org.unizin.cmp.oai.harvester;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;
import org.unizin.cmp.oai.mocks.Mocks;

public final class TestHarvestRestart extends HarvesterTestBase {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testRetryParamsThrowsWithoutHarvest() throws Exception {
        final Harvester harvester = defaultTestHarvester();
        exception.expect(IllegalStateException.class);
        harvester.getRetryParams();
    }

    @Test
    public void testRestartParametersWithoutToken() throws Exception {
        mockHttpClient.addResponseFrom(HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "Some transient error.",
                "Look. Something went wrong.");
        final HarvestParams params = defaultTestParams();
        final Harvester harvester = defaultTestHarvester();
        final OAIResponseHandler responseHandler = Mocks.newResponseHandler();
        try {
            harvester.start(params, responseHandler);
        } catch (final HarvesterException e) {
            Assert.assertEquals(params, harvester.getRetryParams());
        }
    }

    @Test
    public void testRestartParmetersWithToken() throws Exception {
        mockHttpClient.addResponseFrom(200, "", getClass().getResourceAsStream(
                "/oai-responses/oai-partial-list-records-response.xml"));
        mockHttpClient.addResponseFrom(500, "Shrug.",
                "Yet more badness.");
        final HarvestParams params = defaultTestParams();
        final Harvester harvester = defaultTestHarvester();
        final OAIResponseHandler responseHandler = Mocks.newResponseHandler();
        exception.expect(HarvesterException.class);
        try {
            harvester.start(params, responseHandler);
        } catch (final HarvesterException e) {
            /*
             * Not getting the expected results? Check for unexpected
             * HarvesterExceptions, i.e., XML parsing errors.
             */
            final String expectedToken =
                    "0001-01-01T00:00:00Z/9999-12-31T23:59:59Z//oai_dc/100";
            final HarvestParams expected = defaultTestParams()
                    .withResumptionToken(expectedToken);
            Assert.assertEquals(expected, harvester.getRetryParams());
            throw e;
        }
    }
}
