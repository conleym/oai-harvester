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
import org.unizin.cmp.oai.harvester.ListResponses;
import org.unizin.cmp.oai.harvester.Tests;
import org.unizin.cmp.oai.harvester.WireMockUtils;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public final class TestMergingHandler {
    @Rule
    public final WireMockRule wireMock = WireMockUtils.newWireMockRule();
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final String expected;

    public TestMergingHandler() throws IOException {
        expected = IOUtils.stringFromClasspathFile(
                "/oai-expected/merged-list-records.xml");
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreWhitespace(true);
    }

    private void test(final Harvester harvester, final HarvestParams params)
            throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        harvester.start(params, new MergingOAIResponseHandler(
                Tests.simpleMergingHandler(baos)));
        XMLAssert.assertXMLEqual(expected, new String(baos.toByteArray(),
                StandardCharsets.UTF_8));
    }

    @Test
    public void testSingleResponse() throws Exception {
        WireMockUtils.getStub(expected);
        test(new Harvester.Builder().build(), defaultTestParams().build());
    }

    @Test
    public void testMultipleResponses() throws Exception {
        ListResponses.setupWithDefaultListRecordsResponse(true);
        final Harvester harvester = new Harvester.Builder().build();
        test(harvester, defaultTestParams().build());
    }
}
