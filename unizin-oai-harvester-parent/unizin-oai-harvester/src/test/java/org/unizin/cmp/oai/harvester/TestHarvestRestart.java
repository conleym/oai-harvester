package org.unizin.cmp.oai.harvester;

import static org.unizin.cmp.oai.harvester.Tests.defaultTestParams;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;
import org.unizin.cmp.oai.mocks.MockHttpClient;
import org.unizin.cmp.oai.mocks.Mocks;

public final class TestHarvestRestart {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testRetryParamsThrowsWithoutHarvest() throws Exception {
        final Harvester harvester = new Harvester.Builder().build();
        exception.expect(IllegalStateException.class);
        harvester.getRetryParams();
    }

    /**
     * Tests that retry parameters do not change if the harvester receives no
     * resumption token.
     */
    @Test
    public void testRestartParametersWithoutToken() throws Exception {
        Tests.testWithWiremockServer(() -> {
            Tests.createWiremockStubForGetResponse(
                    HttpStatus.SC_INTERNAL_SERVER_ERROR,
                    "Look. Something went wrong.");
            final HarvestParams params = defaultTestParams();
            final Harvester harvester = new Harvester.Builder().build();
            final OAIResponseHandler responseHandler = Mocks.newResponseHandler();
            try {
                harvester.start(params, responseHandler);
            } catch (final HarvesterException e) {
                Assert.assertEquals(params, harvester.getRetryParams());
            }
        });
    }

    /**
     * Tests that the retry parameters contain a resumption token sent by the
     * repository.
     */
    @Test
    public void testRestartParmetersWithToken() throws Exception {
        final MockHttpClient mockHttpClient = new MockHttpClient();
        mockHttpClient.addResponseFrom(HttpStatus.SC_OK, "",
                getClass().getResourceAsStream(
                "/oai-responses/oai-partial-list-records-response.xml"));
        mockHttpClient.addResponseFrom(HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "Shrug.", "Yet more badness.");
        final HarvestParams params = defaultTestParams();
        final Harvester harvester = new Harvester.Builder()
                .withHttpClient(mockHttpClient)
                .build();
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
