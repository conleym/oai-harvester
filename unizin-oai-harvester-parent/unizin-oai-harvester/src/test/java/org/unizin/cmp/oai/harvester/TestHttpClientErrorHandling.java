package org.unizin.cmp.oai.harvester;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.unizin.cmp.oai.harvester.Tests.defaultTestParams;
import static org.unizin.cmp.oai.mocks.Mocks.inOrderVerify;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;
import org.unizin.cmp.oai.mocks.Mocks;
import org.unizin.cmp.oai.mocks.NotificationMatchers;

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
                .start(defaultTestParams().build(), h);
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
        stubFor(get(urlMatching(".*"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_BAD_GATEWAY)));
        exception.expect(HarvesterException.class);
        exception.expectMessage(CoreMatchers.startsWith(
                String.format("Got HTTP status %d for request",
                        HttpStatus.SC_BAD_GATEWAY)));
        final OAIResponseHandler h = Mocks.newResponseHandler();
        try {
            new Harvester.Builder().build().start(defaultTestParams().build(),
                    h);
        } catch (final Exception e) {
            verifyResponseHandler(h);
            throw e;
        }
    }


    /**
     * Tests notifications and exception handling when the harvester's request
     * factory throws an exception.
     * <p>
     * Not strictly an {@code HttpClient} thing, but the results should be very
     * similar, so it's convenient to include this test here.
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
            harvester.start(defaultTestParams().build(), h);
        } catch (final Exception e) {
            verifyResponseHandler(h);
            throw e;
        }
    }
}
