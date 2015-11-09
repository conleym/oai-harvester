package org.unizin.cmp.oai.harvester.response;

import static org.unizin.cmp.oai.harvester.IOUtils.stringFromStream;
import static org.unizin.cmp.oai.harvester.Tests.defaultTestParams;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.unizin.cmp.oai.harvester.Harvester;
import org.unizin.cmp.oai.harvester.TestListResponses;
import org.unizin.cmp.oai.harvester.Tests;
import org.unizin.cmp.oai.harvester.exception.OAIProtocolException;
import org.unizin.cmp.oai.mocks.MockHttpClient;
import org.unizin.cmp.oai.mocks.MockHttpResponse;
import org.xml.sax.SAXException;

public final class TestFilesHandler {

    @Rule
    public final TemporaryFolder tempDir = new TemporaryFolder();

    @Rule
    public final ExpectedException exception = ExpectedException.none();


    private MockHttpClient mockHttpClient;

    @Before
    public void initMockHttpClient() {
        mockHttpClient = new MockHttpClient();
    }

    private Harvester newHarvester() {
        return new Harvester.Builder()
                .withHttpClient(mockHttpClient)
                .build();
    }

    private static void fileEquals(final String expected,
            final File file) throws IOException, SAXException {
        final String fileStr = stringFromStream(new FileInputStream(file));
        XMLAssert.assertXMLEqual(expected, fileStr);
    }

    private void fileAssertions() throws IOException, SAXException {
        final File[] files = tempDir.getRoot().listFiles(
                (f) -> f.getName().endsWith(".xml"));
        Assert.assertEquals(mockHttpClient.getResponses().size(), files.length);
        Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
        final Iterator<MockHttpResponse> respIter =
                mockHttpClient.getResponses().iterator();
        long counter = 1;
        for (final File file : files) {
            Assert.assertEquals(String.format("%d.xml", counter),
                    file.getName());
            fileEquals(respIter.next().getEntityContent(), file);
            counter++;
        }
    }


    @Test
    public void testList() throws Exception {
        TestListResponses.setupWithDefaultListRecordsResponse(true,
                mockHttpClient);
        FilesOAIResponseHandler handler =
                new FilesOAIResponseHandler(tempDir.getRoot());
        final Harvester harvester = newHarvester();
        harvester.start(defaultTestParams(), handler);
        fileAssertions();
    }

    @Test
    public void testOAIProtocolError() throws Exception {
        Tests.setupWithDefaultError(mockHttpClient);
        final FilesOAIResponseHandler handler =
                new FilesOAIResponseHandler(tempDir.getRoot());
        final Harvester harvester = newHarvester();
        exception.expect(OAIProtocolException.class);
        try {
            harvester.start(defaultTestParams(), handler);
        } catch (final OAIProtocolException e) {
            fileAssertions();
            throw e;
        }
    }
}
