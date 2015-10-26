package org.unizin.cmp.oai.harvester.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpStatus;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.harvester.HarvestParams;
import org.unizin.cmp.oai.harvester.Harvester;
import org.unizin.cmp.oai.harvester.HarvesterTestBase;
import org.unizin.cmp.oai.harvester.TestListResponses;
import org.unizin.cmp.oai.harvester.Utils;

public final class TestMergingHandler extends HarvesterTestBase {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final String expected;

    public TestMergingHandler() throws IOException {
        expected = Utils.fromStream(Utils.fromClasspathFile(
                "/oai-expected/merged-list-records.xml"));
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreWhitespace(true);
    }

    private void test(final Harvester harvester, final HarvestParams params)
            throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        harvester.start(params, new MergingOAIResponseHandler(baos));
        XMLAssert.assertXMLEqual(expected, new String(baos.toByteArray(),
                StandardCharsets.UTF_8));
    }

    @Test
    public void testSingleResponse() throws Exception {
        mockHttpClient.addResponseFrom(HttpStatus.SC_OK, "", expected);
        final Harvester harvester = defaultTestHarvester();
        final HarvestParams params = defaultTestParams();
        test(harvester, params);
    }

    @Test
    public void testMultipleResponses() throws Exception {
        TestListResponses.setupWithDefaultListRecordsResponse(true,
                mockHttpClient);
        final Harvester harvester = defaultTestHarvester();
        final HarvestParams params = defaultTestParams();
        test(harvester, params);
    }
}
