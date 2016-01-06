package org.unizin.cmp.oai.harvester;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.unizin.cmp.oai.harvester.Tests.newParams;
import static org.unizin.cmp.oai.mocks.Mocks.inOrderVerify;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observer;

import javax.xml.stream.XMLStreamException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.OAIError;
import org.unizin.cmp.oai.OAIErrorCode;
import org.unizin.cmp.oai.harvester.exception.HarvesterXMLParsingException;
import org.unizin.cmp.oai.harvester.exception.OAIProtocolException;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;
import org.unizin.cmp.oai.mocks.Mocks;
import org.unizin.cmp.oai.mocks.NotificationMatchers;
import org.unizin.cmp.oai.templates.ErrorsTemplate;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.ByteStreams;

import freemarker.template.TemplateException;


public final class TestOAIProtocolErrorHandling {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /** WireMock is used for all but one test, so we'll just use the rule. */
    @Rule
    public final WireMockRule wireMock = WireMockUtils.newWireMockRule();

    private static void setupWithError()
            throws TemplateException, IOException {
        final String errorResponse = ErrorsTemplate.process();
        WireMockUtils.getStub(errorResponse);
    }

    private static void setupWithError(final String errorResponse) {
        WireMockUtils.getStub(errorResponse);
    }

    private Throwable checkSingleSuppressedException(final Throwable t) {
        final Throwable[] suppressed = t.getSuppressed();
        Assert.assertEquals(1, suppressed.length);
        return suppressed[0];
    }

    private void simpleTest(final List<OAIError> errors)
            throws Exception {
        final String errorResponse = ErrorsTemplate.process(errors);
        setupWithError(errorResponse);
        exception.expect(OAIProtocolException.class);
        try {
            new Harvester.Builder().build().start(newParams().build(),
                    Mocks.newResponseHandler());
        } catch (final OAIProtocolException e) {
            Assert.assertEquals(errors, e.getOAIErrors());
            throw e;
        }
    }

    /**
     * Tests that an error response with a single &lt;error&gt; element is
     * processed correctly.
     */
    @Test
    public void testSingleError() throws Exception {
        simpleTest(ErrorsTemplate.defaultErrorList());
    }

    /**
     * Tests that an error response with multiple &lt;error&gt; elements is
     * processed correctly.
     */
    @Test
    public void testMultipleErrors() throws Exception {
        List<OAIError> errors = Arrays.asList(
                new OAIError(OAIErrorCode.BAD_RESUMPTION_TOKEN.code(),
                        "Some message about the resumption token"),
                new OAIError(OAIErrorCode.BAD_ARGUMENT.code()),
                new OAIError(OAIErrorCode.CANNOT_DISSEMINATE_FORMAT.code(),
                        "Your format is bad and you should feel bad"));
        simpleTest(errors);
    }

    /**
     * Tests that an error response containing a nonstandard error code is
     * handled correctly.
     */
    @Test
    public void testNonStandardError() throws Exception {
        List<OAIError> errors = Arrays.asList(
                new OAIError("nonstandardErrorCode"),
                new OAIError(OAIErrorCode.NO_SET_HIERARCHY.code(), "Hi!"));
        simpleTest(errors);
    }

    /**
     * Tests that XML parsing exceptions that occur while processing an error
     * response are added as suppressed exceptions to an appropriate
     * {@code OAIProtocolException}.
     * <p>
     * Somewhat obviously, only errors that are processed before the XML parse
     * error occurred can be reported.
     * </p>
     */
    @Test
    public void testPriorityOverParseErrors() throws Exception {
        final String errorResponse = ErrorsTemplate.process(
                ErrorsTemplate.defaultErrorList()) +
                " some extra content making the XML invalid.";
        setupWithError(errorResponse);
        exception.expect(OAIProtocolException.class);
        try {
            new Harvester.Builder().build().start(newParams().build(),
                    Mocks.newResponseHandler());
        } catch (final OAIProtocolException e) {
            Assert.assertEquals(ErrorsTemplate.defaultErrorList(),
                    e.getOAIErrors());
            final Throwable suppressed = checkSingleSuppressedException(e);
            Assert.assertTrue(
                    suppressed instanceof HarvesterXMLParsingException);
            throw e;
        }
    }


    private void checkXercesMultipleExceptions(final Throwable probablyIO,
            final Throwable probablyXML) {
        Mocks.assertTestException(probablyIO, IOException.class);
        Assert.assertTrue(probablyXML instanceof HarvesterXMLParsingException);
        Assert.assertTrue(probablyXML.getCause() instanceof XMLStreamException);
    }

    /**
     * Tests that, if a stream containing an error response from the server
     * throws an {@link IOException} when closed, that this exception is added
     * as a suppressed exception to the {@code OAIProtocolException}.
     * <p>
     * Does not use WireMock.
     * </p>
     */
    @Test
    public void testPriorityOverStreamClosingErrors() throws Exception {
        final String arbitraryValidOAIResponse = ErrorsTemplate.process();
        InputStream stream = Mocks.throwsWhenClosed(IOUtils.streamFromString(
                arbitraryValidOAIResponse));
        final HttpClient httpClient = mock(HttpClient.class);
        final HttpResponse response = mock(HttpResponse.class);
        final HttpEntity entity = mock(HttpEntity.class);
        final StatusLine sl = new BasicStatusLine(HttpVersion.HTTP_1_1,
                HttpStatus.SC_OK, "");
        when(response.getStatusLine()).thenReturn(sl);
        when(response.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(stream);
        when(httpClient.execute(any())).thenReturn(response);
        final Harvester harvester = new Harvester.Builder()
                .withHttpClient(httpClient)
                .build();
        exception.expect(OAIProtocolException.class);
        try {
            harvester.start(newParams().build(), Mocks.newResponseHandler());
        } catch (final OAIProtocolException e) {
            Assert.assertEquals(ErrorsTemplate.defaultErrorList(),
                    e.getOAIErrors());
            if (StAXImplementation.DEFAULT.isJDKOrXerces()) {
                /*
                 * Somehow Xerces (and its JDK derivative) close the stream
                 * early and get multiple suppressed exceptions, one
                 * XMLStreamException while trying to read the next event, plus
                 * the expected IOException. How stupid!
                 */
                final Throwable[] suppressed = e.getSuppressed();
                Assert.assertEquals(2, suppressed.length);
                Throwable io;
                Throwable xml;
                if (suppressed[0] instanceof IOException) {
                    io = suppressed[0];
                    xml = suppressed[1];
                } else {
                    io = suppressed[1];
                    xml = suppressed[0];
                }
                checkXercesMultipleExceptions(io, xml);
            } else {
                // Woodstox, at least, does what seems like the right thing.
                final Throwable suppressed = checkSingleSuppressedException(e);
                Mocks.assertTestException(suppressed, IOException.class);
            }
            throw e;
        }
    }

    @Test
    public void testPriorityOverResponseHandlerErrors() throws Exception {
        setupWithError();
        final OAIResponseHandler h = Mocks.newResponseHandler();
        doThrow(new IllegalArgumentException(Mocks.TEST_EXCEPTION_MESSAGE))
            .when(h).onHarvestEnd(any());
        exception.expect(OAIProtocolException.class);
        try {
            new Harvester.Builder().build().start(newParams().build(),
                    h);
        } catch (final OAIProtocolException e) {
            final Throwable suppressed = checkSingleSuppressedException(e);
            Mocks.assertTestException(suppressed,
                    IllegalArgumentException.class);
            throw e;
        }
    }

    @Test
    public void testOAIHandlerCallsAreMade() throws Exception {
        setupWithError();
        final OAIResponseHandler h = Mocks.newResponseHandler();
        exception.expect(OAIProtocolException.class);
        try {
            new Harvester.Builder().build().start(newParams().build(),
                    h);
        } catch (final OAIProtocolException e) {
            /*
             * Each of these should be called _exactly_ once, and in precisely
             * this order.
             */
            inOrderVerify(h).onHarvestStart(
                    NotificationMatchers.harvestStarted());
            inOrderVerify(h).onResponseReceived(
                    NotificationMatchers.responseReceived());
            inOrderVerify(h).getEventHandler(
                    NotificationMatchers.responseReceived());
            inOrderVerify(h).onResponseProcessed(
                    NotificationMatchers.responseProcessedWithError());
            inOrderVerify(h).onHarvestEnd(
                    NotificationMatchers.harvestEndedWithError());
            throw e;
        }
    }

    @Test
    public void testObserverReceivesNotifications() throws Exception {
        setupWithError();
        final Observer observer = mock(Observer.class);
        final Harvester harvester = new Harvester.Builder().build();
        harvester.addObserver(observer);
        exception.expect(OAIProtocolException.class);
        try {
            harvester.start(newParams().build(),
                    Mocks.newResponseHandler());
        } catch (final OAIProtocolException e) {
            inOrderVerify(observer).update(eq(harvester),
                    NotificationMatchers.harvestStarted());
            inOrderVerify(observer).update(eq(harvester),
                    NotificationMatchers.responseReceived());
            inOrderVerify(observer).update(eq(harvester),
                    NotificationMatchers.responseProcessedWithError());
            inOrderVerify(observer).update(eq(harvester),
                    NotificationMatchers.harvestEndedWithError());
            throw e;
        }
    }

    private void serialize(final OAIProtocolException ope) throws Exception {
        ObjectOutputStream oos = new ObjectOutputStream(
                ByteStreams.nullOutputStream());
        oos.writeObject(ope);
    }

    @Test
    public void testSerialization() throws Exception {
        serialize(new OAIProtocolException(Collections.emptyList()));
        List<OAIError> errors = Arrays.asList(
                new OAIError("nonstandardErrorCode"),
                new OAIError(OAIErrorCode.BAD_ARGUMENT.code(), "Hi!"));
        serialize(new OAIProtocolException(errors));
    }
}
