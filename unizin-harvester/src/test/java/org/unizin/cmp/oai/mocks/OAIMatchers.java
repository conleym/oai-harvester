package org.unizin.cmp.oai.mocks;

import java.util.function.Predicate;

import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;

public final class OAIMatchers {
	public static Object fromPredicate(final Predicate<Object> predicate) {
		return fromPredicate(predicate, Object.class);
	}
	
	/**
	 * Create an argument matcher from a {@link Predicate}.
	 * <p>
	 * The type of the argument will be checked.
	 * 
	 * @param predicate the predicate to use for matching.
	 * @param clazz the type of the argument.
	 */
	public static <T> T fromPredicate(
			final Predicate<T> predicate, Class<T> clazz) {
		return Matchers.argThat(new ArgumentMatcher<T>() {
			@Override
			public boolean matches(final Object argument) {
				return clazz.isInstance(argument) &&
						predicate.test(clazz.cast(argument));
			}
		});
	}
	
	/**
	 * A version of {@link Matchers#any(Class)} that checks the type of its
	 * argument, which the former explicitly does not.
	 */
	public static <T> T anyOfType(final Class<T> clazz) {
		return Matchers.argThat(new ArgumentMatcher<T>() {
			@Override
			public boolean matches(final Object argument) {
				return clazz.isInstance(argument);
			}
		});
	}
	
	/** No instances allowed. */
	private OAIMatchers() {}
}
