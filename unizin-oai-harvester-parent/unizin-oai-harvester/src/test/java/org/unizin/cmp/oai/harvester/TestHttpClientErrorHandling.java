package org.unizin.cmp.oai.harvester;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.unizin.cmp.oai.harvester.Tests.defaultTestParams;
import static org.unizin.cmp.oai.mocks.Mocks.inOrderVerify;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;
import org.unizin.cmp.oai.mocks.MockHttpClient;
import org.unizin.cmp.oai.mocks.Mocks;
import org.unizin.cmp.oai.mocks.NotificationMatchers;

public final class TestHttpClientErrorHandling {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private MockHttpClient mockHttpClient;

    @Before
    public void initMockHttpClient() {
        mockHttpClient = new MockHttpClient();
    }

    private Harvester newHarvester() {
        return new Harvester.Builder().withHttpClient(mockHttpClient).build();
    }

    private static void verifyResponseHandler(final OAIResponseHandler h) {
        inOrderVerify(h).onHarvestStart(NotificationMatchers
                .harvestStarted());
        inOrder(h).verify(h, times(0)).onResponseReceived(any());
        inOrder(h).verify(h, times(0)).getEventHandler(any());
        inOrder(h).verify(h, times(0)).onResponseProcessed(any());
        inOrderVerify(h).onHarvestEnd(NotificationMatchers
                .harvestEndedWithError());
    }

    /**
    * Tests that {@code RuntimeExceptions} thrown by {@code HttpClient} are
    * propagated to the caller.
    */
    @Test
    public void testHttpClientRuntimeExceptions() throws Exception {
        mockHttpClient.setRuntimeException(
                new NullPointerException(Mocks.TEST_EXCEPTION_MESSAGE));
        final OAIResponseHandler h = Mocks.newResponseHandler();
        exception.expect(NullPointerException.class);
        exception.expectMessage(Mocks.TEST_EXCEPTION_MESSAGE);
        try {
            newHarvester().start(defaultTestParams(), h);
        } catch (final Exception e) {
            verifyResponseHandler(h);
            throw e;
        }
    }

    /**
    * Tests that {@code IOExceptions} thrown by {@code HttpClient} are wrapped
    * in {@code UncheckedIOExceptions} and propagated to the caller.
    */
    @Test
    public void testHttpClientCheckedExceptions() throws Exception {
        mockHttpClient.setCheckedException(new IOException(
                Mocks.TEST_EXCEPTION_MESSAGE));
        final OAIResponseHandler h = Mocks.newResponseHandler();
        exception.expect(UncheckedIOException.class);
        try {
            newHarvester().start(defaultTestParams(), h);
        } catch (final UncheckedIOException e) {
            final Throwable cause = e.getCause();
            Mocks.assertTestException(cause, IOException.class);
            verifyResponseHandler(h);
            throw e;
        }
    }

    /**
     * Test notifications and exception handling when the server returns a
     * status other than SC_OK.
     */
    @Test
    public void testNotOKStatus() throws Exception {
        mockHttpClient.addResponseFrom(HttpStatus.SC_BAD_GATEWAY,
                "Something's wrong", "");
        exception.expect(HarvesterException.class);
        exception.expectMessage(CoreMatchers.startsWith(
                String.format("Got HTTP status %d for request",
                        HttpStatus.SC_BAD_GATEWAY)));
        final OAIResponseHandler h = Mocks.newResponseHandler();
        try {
            newHarvester().start(defaultTestParams(), h);
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
            harvester.start(defaultTestParams(), h);
        } catch (final Exception e) {
            verifyResponseHandler(h);
            throw e;
        }
    }
}
