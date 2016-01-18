package org.unizin.cmp.oai.harvester;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.unizin.cmp.oai.harvester.Tests.newParams;
import static org.unizin.cmp.oai.mocks.Mocks.inOrderVerify;
import static org.unizin.cmp.oai.mocks.WireMockUtils.getAnyURL;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicStatusLine;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.harvester.exception.HarvesterHTTPStatusException;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;
import org.unizin.cmp.oai.mocks.Mocks;
import org.unizin.cmp.oai.mocks.NotificationMatchers;
import org.unizin.cmp.oai.mocks.WireMockUtils;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public final class TestHttpClientErrorHandling {
    @Rule
    public final WireMockRule wireMock = WireMockUtils.newWireMockRule();

    @Rule
    public final ExpectedException exception = ExpectedException.none();


    private static void verifyResponseHandler(final OAIResponseHandler h) {
        inOrderVerify(h).onHarvestStart(NotificationMatchers
                .harvestStarted());
        inOrder(h).verify(h, never()).onResponseReceived(any());
        inOrder(h).verify(h, never()).getEventHandler(any());
        inOrder(h).verify(h, never()).onResponseProcessed(any());
        inOrderVerify(h).onHarvestEnd(NotificationMatchers
                .harvestEndedWithError());
    }

    private void testExecuteException(final Class<? extends Throwable> thrown,
            final Class<? extends Throwable> expected,
            final Consumer<Throwable> verifyExpectations)
                    throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        final Throwable t = thrown.getConstructor(String.class)
                .newInstance(Mocks.TEST_EXCEPTION_MESSAGE);
        when(httpClient.execute(any())).thenThrow(t);
        final OAIResponseHandler h = Mocks.newResponseHandler();
        exception.expect(expected);
        try {
            new Harvester.Builder().withHttpClient(httpClient).build()
            .start(newParams().build(), h);
        } catch (final Exception e) {
            verifyResponseHandler(h);
            verifyExpectations.accept(e);
            throw e;
        }
    }

    /**
     * Tests that {@code RuntimeExceptions} thrown by {@code HttpClient} are
     * propagated to the caller.
     */
    @Test
    public void testHttpClientRuntimeExceptions() throws Exception {
        final Class<NullPointerException> npe = NullPointerException.class;
        testExecuteException(npe, npe, (e) -> {
            Assert.assertEquals(Mocks.TEST_EXCEPTION_MESSAGE, e.getMessage());
        });
    }

    /**
     * Tests that {@code IOExceptions} thrown by {@code HttpClient} are wrapped
     * in {@code UncheckedIOExceptions} and propagated to the caller.
     */
    @Test
    public void testHttpClientCheckedExceptions() throws Exception {
        final Class<IOException> io = IOException.class;
        final Class<UncheckedIOException> uio = UncheckedIOException.class;
        testExecuteException(io, uio, (e) -> {
            final Throwable cause = e.getCause();
            Mocks.assertTestException(cause, IOException.class);
        });
    }

    /**
     * Test notifications and exception handling when the server returns a
     * status other than SC_OK.
     */
    @Test
    public void testNotOKStatus() throws Exception {
        final int status = HttpStatus.SC_BAD_GATEWAY;
        final MappingBuilder mb = getAnyURL()
                .willReturn(aResponse()
                        .withBodyFile("error.txt")
                        .withStatus(status)
                        .withHeader("content-type", "text/plain"));
        stubFor(mb);
        exception.expect(HarvesterHTTPStatusException.class);
        exception.expectMessage(CoreMatchers.startsWith(
                String.format("Got HTTP status %d for request", status)));
        final OAIResponseHandler h = Mocks.newResponseHandler();
        try {
            new Harvester.Builder().build().start(newParams().build(),
                    h);
        } catch (final Exception e) {
            verifyResponseHandler(h);
            Assert.assertTrue(e.getSuppressed().length == 0);
            if (e instanceof HarvesterHTTPStatusException) {
                final HarvesterHTTPStatusException hhse =
                        (HarvesterHTTPStatusException)e;
                System.out.println(hhse.getStatusLine());
                Assert.assertEquals(hhse.getStatusLine().getStatusCode(),
                        status);
                System.out.println(Arrays.asList(hhse.getHeaders()));
                final Stream<Boolean> sb = Arrays.stream(hhse.getHeaders())
                        .map(x -> "content-type".equalsIgnoreCase(x.getName())
                                && "text/plain".equals(x.getValue()));
                Assert.assertTrue("content-type header should be present with "
                        + "value 'text/plain'.", sb.anyMatch(x -> x));
                checkBody(hhse.getResponseBody());
            }
            throw e;
        }
    }

    private void checkBody(final byte[] bytes) throws Exception {
        final String s = IOUtils.stringFromClasspathFile(
                "/__files/error.txt");
        Assert.assertEquals(s, new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * Tests that, when the server returns a status other than 200 OK, and when
     * there is an IOException trying to read the response body, that the
     * exception is an HTTP status exception with the IOException suppressed.
     * <p>Does not use WireMock.</p>
     */
    @Test
    public void testNotOKStatusWithIOException() throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        final HttpResponse response = mock(HttpResponse.class);
        final HttpEntity entity = mock(HttpEntity.class);
        final StatusLine sl = new BasicStatusLine(HttpVersion.HTTP_1_1,
                HttpStatus.SC_INTERNAL_SERVER_ERROR, "");
        when(response.getStatusLine()).thenReturn(sl);
        when(response.getEntity()).thenReturn(entity);
        // Our implementation calls entity.writeTo, but stub both writeTo and
        // getContent in case the implementation changes some day.
        final IOException io = new IOException(Mocks.TEST_EXCEPTION_MESSAGE);
        when(entity.getContent()).thenThrow(io);
        doThrow(io).when(entity)
        .writeTo(any());

        when(httpClient.execute(any())).thenReturn(response);
        final Harvester harvester = new Harvester.Builder()
                .withHttpClient(httpClient)
                .build();
        exception.expect(HarvesterHTTPStatusException.class);
        final OAIResponseHandler h = Mocks.newResponseHandler();
        try {
            harvester.start(newParams().build(), h);
        } catch (final HarvesterHTTPStatusException e) {
            Assert.assertEquals(1,  e.getSuppressed().length);
            Assert.assertTrue(e.getSuppressed()[0] instanceof IOException);
            throw e;
        }
    }

    /**
     * Tests notifications and exception handling when the harvester's request
     * factory throws an exception.
     * <p>
     * Not strictly an {@code HttpClient} thing, but the results should be very
     * similar, so it's convenient to include this test here.
     * </p>
     */
    @Test
    public void testRequestFactoryException() throws Exception {
        final OAIResponseHandler h = Mocks.newResponseHandler();
        final Harvester harvester = new Harvester.Builder()
                .withOAIRequestFactory((a, b) -> {
                    throw new RuntimeException(Mocks.TEST_EXCEPTION_MESSAGE);
                })
                .build();
        exception.expect(RuntimeException.class);
        exception.expectMessage(Mocks.TEST_EXCEPTION_MESSAGE);
        try {
            harvester.start(newParams().build(), h);
        } catch (final Exception e) {
            verifyResponseHandler(h);
            throw e;
        }
    }
}
