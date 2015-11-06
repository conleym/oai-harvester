package org.unizin.cmp.oai.harvester.response;

import static org.unizin.cmp.oai.harvester.Tests.defaultTestParams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.harvester.HarvestParams;
import org.unizin.cmp.oai.harvester.Harvester;
import org.unizin.cmp.oai.harvester.IOUtils;
import org.unizin.cmp.oai.harvester.TestListResponses;
import org.unizin.cmp.oai.harvester.Tests;
import org.unizin.cmp.oai.mocks.MockHttpClient;

public final class TestMergingHandler {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final String expected;

    public TestMergingHandler() throws IOException {
        expected = IOUtils.stringFromStream(IOUtils.streamFromClasspathFile(
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
        Tests.testWithWiremockServer(() -> {
            Tests.createWiremockStubForOKGetResponse(expected);
            test(new Harvester.Builder().build(), defaultTestParams());
        });
    }

    @Test
    public void testMultipleResponses() throws Exception {
        final MockHttpClient mockHttpClient = new MockHttpClient();
        TestListResponses.setupWithDefaultListRecordsResponse(true,
                mockHttpClient);
        final Harvester harvester = new Harvester.Builder()
                .withHttpClient(mockHttpClient)
                .build();
        test(harvester, defaultTestParams());
    }
}
