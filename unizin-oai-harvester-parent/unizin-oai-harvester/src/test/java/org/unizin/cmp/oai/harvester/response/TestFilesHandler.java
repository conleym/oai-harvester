package org.unizin.cmp.oai.harvester.response;

import static org.unizin.cmp.oai.harvester.IOUtils.stringFromStream;
import static org.unizin.cmp.oai.harvester.Tests.newParams;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.unizin.cmp.oai.harvester.Harvester;
import org.unizin.cmp.oai.harvester.ListResponses;
import org.unizin.cmp.oai.harvester.WireMockUtils;
import org.unizin.cmp.oai.harvester.exception.OAIProtocolException;
import org.xml.sax.SAXException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public final class TestFilesHandler {
    @Rule
    public final WireMockRule wireMock = WireMockUtils.newWireMockRule();

    @Rule
    public final TemporaryFolder tempDir = new TemporaryFolder();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private static void fileEquals(final String expected,
            final File file) throws IOException, SAXException {
        final String fileStr = stringFromStream(new FileInputStream(file));
        XMLAssert.assertXMLEqual(expected, fileStr);
    }

    private void fileAssertions(final List<String> expected)
            throws IOException, SAXException {
        final File[] files = tempDir.getRoot().listFiles(
                (f) -> f.getName().endsWith(".xml"));
        Assert.assertEquals(expected.size(), files.length);
        Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
        final Iterator<String> respIter = expected.iterator();
        long counter = 1;
        for (final File file : files) {
            Assert.assertEquals(String.format("%d.xml", counter),
                    file.getName());
            fileEquals(respIter.next(), file);
            counter++;
        }
    }

    @Test
    public void testList() throws Exception {
        final List<String> expected = ListResponses
                .setupWithDefaultListRecordsResponse(true);
        FilesOAIResponseHandler handler =
                new FilesOAIResponseHandler(tempDir.getRoot());
        final Harvester harvester = new Harvester.Builder().build();
        harvester.start(newParams().build(), handler);
        fileAssertions(expected);
    }

    @Test
    public void testOAIProtocolError() throws Exception {
        final String expected = WireMockUtils.oaiErrorResponseStub();
        final FilesOAIResponseHandler handler =
                new FilesOAIResponseHandler(tempDir.getRoot());
        final Harvester harvester = new Harvester.Builder().build();
        exception.expect(OAIProtocolException.class);
        try {
            harvester.start(newParams().build(), handler);
        } catch (final OAIProtocolException e) {
            fileAssertions(Collections.singletonList(expected));
            throw e;
        }
    }
}
