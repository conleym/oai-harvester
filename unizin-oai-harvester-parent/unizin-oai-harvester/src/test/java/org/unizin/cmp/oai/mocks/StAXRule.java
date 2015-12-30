package org.unizin.cmp.oai.mocks;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.harvester.StAXImplementation;

/**
 * JUnit rule that runs test methods repeatedly, once for each specified StAX
 * implementation.
 * <p>
 * For example, suppose I have some StAX code that I'd like to test with the
 * JDK's built in implementation, Xerces, and Woodstox. An obvious solution is
 * to modify the classpath and run the tests three times, but this will be
 * redundant for any tests of code that <em>doesn't</em> use StAX. A better
 * solution is to add all the implementations you need to test to the classpath,
 * then run selected tests multiple times, using a different StAX
 * implementation for each run:
 * <pre>
 * public final class Test
 *   // Run each test once for each supported StAX implementation.
 *   @Rule
 *   public final StAXRule stax = StAXRule.useAll();
 *
 *   @Test
 *   public void testStAXStuff() {
 *      // Some code that relies on StAX goes here.
 *   }
 *
 *   // Just run this one once with the JDK's built in implementation.
 *   @Test
 *   @StAXImplementations(StAXImplementation.JDK)
 *   public void testNonStAXStuff() {
 *      // Some code that doesn't rely on StAX.
 *   }
 * </pre>
 * </p>
 */
public final class StAXRule implements TestRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            StAXRule.class);

    /**
     * Annotation allowing per-method overrides of a StAX rule.
     */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface StAXImplementations {
        StAXImplementation[] value();
    }

    private List<StAXImplementation> implementations;

    public StAXRule(final StAXImplementation...implementations) {
        this.implementations = Arrays.asList(implementations);
    }

    public static StAXRule usingAll() {
        return new StAXRule(StAXImplementation.values());
    }

    @Override
    public Statement apply(final Statement base,
            final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final StAXImplementations annotation =
                        description.getAnnotation(StAXImplementations.class);
                List<StAXImplementation> toTest = implementations;
                if (annotation != null &&
                        annotation.value().length > 0) {
                    toTest = Arrays.asList(annotation.value());
                }
                for (final StAXImplementation impl : toTest) {
                    LOGGER.debug("Running {} with StAX implementation {}",
                            description, impl);
                    impl.use();
                    base.evaluate();
                }
            }
        };
    }
}
