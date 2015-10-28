package org.unizin.cmp.oai.mocks;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Predicate;

import org.junit.Assert;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;
import org.unizin.cmp.oai.mocks.ForwardingInputStream.BasicForwardingInputStream;


public final class Mocks {
    private static final Logger LOGGER = LoggerFactory.getLogger(Mocks.class);

    public static final String TEST_EXCEPTION_MESSAGE =
            "Mock exception for testing";

    public static void assertTestException(final Throwable t,
            final Class<? extends Throwable> ofClass) {
        Assert.assertTrue(ofClass.isInstance(t));
        Assert.assertEquals(TEST_EXCEPTION_MESSAGE, t.getMessage());
    }

    public static InputStream throwsWhenClosed(final InputStream delegate) {
        return new BasicForwardingInputStream<InputStream>(delegate){
            @Override
            public void close() throws IOException {
                try {
                    delegate.close();
                } catch (final IOException e) {
                    LOGGER.warn("Ignoring IOException while closing.", e);
                }
                throw new IOException(TEST_EXCEPTION_MESSAGE);
            }
        };
    }

    public static OAIResponseHandler newResponseHandler() {
        final OAIResponseHandler m = mock(OAIResponseHandler.class);
        when(m.getEventHandler(any())).thenReturn(mock(OAIEventHandler.class));
        return m;
    }

    /**
    * Shortcut for the commonly-needed {@code inOrder(mock).verify(mock)}.
    * @param mock a mock object.
    */
    public static <T> T inOrderVerify(final T mock) {
        return inOrder(mock).verify(mock);
    }

    /**
    * Create an argument matcher from a {@link Predicate}.
    * <p>
    * The type of the argument will be checked.
    *
    * @param predicate the predicate to use for matching.
    * @param clazz the type of the argument.
    */
    public static <T> T matcherFromPredicate(
            final Predicate<T> predicate, Class<T> clazz) {
        return Matchers.argThat(new ArgumentMatcher<T>() {
            @Override
            public boolean matches(final Object argument) {
                return clazz.isInstance(argument) &&
                        predicate.test(clazz.cast(argument));
            }
        });
    }
}

