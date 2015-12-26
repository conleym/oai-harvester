package org.unizin.cmp.oai.harvester;

import static org.unizin.cmp.oai.harvester.Tests.defaultTestParams;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;
import org.unizin.cmp.oai.mocks.Mocks;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public final class TestHarvestRestart {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Rule
    public final WireMockRule wireMock = WireMock.newWireMockRule();

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
        WireMock.createWiremockStubForGetResponse(
                HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "Look. Something went wrong.");
        final HarvestParams params = defaultTestParams().build();
        final Harvester harvester = new Harvester.Builder().build();
        try {
            harvester.start(params, Mocks.newResponseHandler());
        } catch (final HarvesterException e) {
            Assert.assertEquals(params, harvester.getRetryParams());
        }
    }

    /**
     * Tests that the retry parameters contain a resumption token sent by the
     * repository.
     */
    @Test
    public void testRestartParmetersWithToken() throws Exception {
        final String expectedToken =
                "0001-01-01T00:00:00Z/9999-12-31T23:59:59Z//oai_dc/100";
        WireMock.createWiremockStubForGetResponse(HttpStatus.SC_OK,
                IOUtils.stringFromClasspathFile(
                        "/oai-responses/oai-partial-list-records-response.xml"),
                WireMock.URL_PATTERN_WITHOUT_RESUMPTION_TOKEN);
        WireMock.createWiremockStubForGetResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "Something's amiss.", WireMock.urlResmptionTokenPattern(expectedToken));
        final Harvester harvester = new Harvester.Builder().build();
        exception.expect(HarvesterException.class);
        try {
            harvester.start(defaultTestParams().build(), Mocks.newResponseHandler());
        } catch (final HarvesterException e) {
            /*
             * Not getting the expected results? Check for unexpected
             * HarvesterExceptions, i.e., XML parsing errors.
             */
            final HarvestParams expected = defaultTestParams()
                    .withResumptionToken(expectedToken)
                    .build();
            Assert.assertEquals(expected, harvester.getRetryParams());
            throw e;
        }
    }
}
