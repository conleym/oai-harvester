package org.unizin.cmp.oai.harvester;

import org.unizin.cmp.oai.harvester.exception.HarvesterException;

/** Lambda utilities. */
public final class Functions {

    /**
     * A void function taking no arguments that can throw checked exceptions.
     */
    @FunctionalInterface
    public static interface CheckedRunnable {
        void run() throws Exception;
    }

    /**
     * A void function taking a single argument that can throw checked
     * exceptions.
     *
     * @param <T>
     *            the type of the function's argument.
     */
    @FunctionalInterface
    public static interface CheckedConsumer<T> {
        void apply(T t) throws Exception;
    }

    /**
     * Convert checked exceptions to {@link HarvesterException
     * HarvesterExceptions}.
     *
     * @param e
     *            an exception.
     * @return {@code e} if it is a runtime exception, or a new
     *         {@code HarvesterException} with {@code e} as its cause if it is
     *         checked.
     */
    private static RuntimeException checkedToRuntime(final Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException)e;
        }
        return new HarvesterException(e);
    }

    /**
     * Wrap a runnable that can throw checked exceptions in one that can't.
     *
     * @param runnable
     *            the checked-exception-throwing function to wrap.
     * @return a function wrapping the original that throws only runtime
     *         exceptions.
     */
    public static Runnable wrap(final CheckedRunnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (final Exception e) {
                throw checkedToRuntime(e);
            }
        };
    }

    /**
     * Wrap a call to void function taking one argument that can throw checked
     * exceptions in a {@code Runnable}.
     *
     * @param consumer
     *            the checked-exception-throwing function to wrap.
     * @param t
     *            the argument to pass to the given function.
     * @return a function that calls the given function with the given argument
     *         and throws only runtime exceptions.
     */
    public static <T> Runnable wrap(final CheckedConsumer<T> consumer,
            final T t) {
        return wrap(() -> consumer.apply(t));
    }

    /**
     * Safely run some code requiring a {@code finally} block without losing
     * exceptions.
     * <p>
     * Suppose an exception, {@code E}, is thrown in a {@code try} block. We
     * need to run some code in the corresponding {@code finally} block that may
     * <em>also</em> throw an exception, {@code F}, but we don't want to lose
     * {@code E}.
     * </p>
     * <p>
     * There are four possibilities to consider:
     * <ol>
     * <li>Neither {@code E} nor {@code F} is thrown. This method will not
     * throw.
     * <li>{@code E} and {@code F} are both thrown. This method will throw
     * {@code E}, with {@code F} attached as a suppressed exception.
     * <li>Only {@code E} is thrown. This method will throw {@code E}.
     * <li>Only {@code F} is thrown. This method will throw {@code F}.
     * </ol>
     * </p>
     *
     * @param tryCall
     *            the code to run inside a {@code try} block.
     * @param finallyCall
     *            the code to run inside the corresponding {@code finally}
     *            block.
     */
    public static void suppressExceptions(final Runnable tryCall,
            final Runnable finallyCall) {
        RuntimeException caught = null;
        try {
            tryCall.run();
        } catch (final RuntimeException bodyEx) {
            caught = bodyEx;
        } finally {
            try {
                finallyCall.run();
            } catch (final RuntimeException finallyEx) {
                if (caught == null) {
                    throw finallyEx;
                } else {
                    caught.addSuppressed(finallyEx);
                    throw caught;
                }
            }
            /*
             * Finally block had no exceptions. Might still have to throw try
             * block's exception.
             */
            if (caught != null) {
                throw caught;
            }
        }
    }

    /** No instances allowed. */
    private Functions() { }
}
