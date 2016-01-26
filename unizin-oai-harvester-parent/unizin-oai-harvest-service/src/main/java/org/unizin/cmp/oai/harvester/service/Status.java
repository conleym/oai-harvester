package org.unizin.cmp.oai.harvester.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.unizin.cmp.oai.ResumptionToken;


/**
 * Utilities for creating status service responses.
 */
final class Status {
    private static <T, U> Optional<U> withOptional(final Optional<T> t,
            final Function<T, U> fun) {
        if (t.isPresent()) {
            return Optional.of(fun.apply(t.get()));
        }
        return Optional.empty();
    }

    /**
     * Format an optional stack trace.
     *
     * @param optional
     *            an optional exception.
     * @return an optional containing the exception's stack trace if it is
     *         present, and an empty optional otherwise.
     */
    static Optional<String> formatStackTrace(
            final Optional<Exception> optional) {
        return withOptional(optional, ex -> {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            return sw.toString();
        });
    }

    /**
     * Format a Java 8 instant according to ISO8601.
     *
     * @param instant
     *            the instant to format.
     * @return the instant as an ISO datetime string.
     * @see DateTimeFormatter.ISO_INSTANT
     */
    static String formatInstant(final Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    /**
     * Format an optional instant.
     *
     * @param optional
     *            the optional instant.
     * @return an optional containing the instant formatted with
     *         {@link #formatInstant(Instant)} if it is present, or an empty
     *         optional otherwise.
     */
    static Optional<String> formatInstant(final Optional<Instant> optional) {
        return withOptional(optional, instant -> formatInstant(instant));
    }

    /**
     * Format an optional URI.
     *
     * @param optional
     *            the optional URI.
     * @return an optional containing the URI as a string if it is present or an
     *         empty optional otherwise.
     */
    static Optional<String> formatURI(final Optional<URI> optional) {
        return withOptional(optional, uri -> uri.toString());
    }

    /**
     * Format an optional map.
     *
     * @param optional
     *            the optional map.
     * @return an optional containing the map as a string if it is present or an
     *         empty optional otherwise.
     */
    static <T,U> Optional<String> formatMap(
            final Optional<? extends Map<T,U>> optional) {
        return withOptional(optional, map -> map.toString());
    }


    /**
     * Convert an optional resumption token to an optional map suitable for
     * Jackson serialization to web service clients.
     * <p>
     * If the token has an expiration date, it will be formatted via
     * {@link #formatInstant(Instant)} before being added to the resulting map.
     * </p>
     *
     * @param optional
     *            the optional resumption token.
     * @return an optional containing the token as a map if present, or an empty
     *         optional otherwise.
     */
    static Optional<Map<String, Object>> convertResumptionToken(
            final Optional<ResumptionToken> optional) {
        return withOptional(optional, token -> {
            final Map<String, Object> m = token.toMap();
            final Optional<Instant> exp = token.getExpirationDate();
            addIfPresent(formatInstant(exp), "expirationDate", m);
            return m;
        });
    }

    /**
     * Add an optional's value to a map only if it is present.
     *
     * @param optional
     *            the optional to conditionally add.
     * @param key
     *            the key.
     * @param map
     *            the map to conditionally add the optional's value to.
     */
    static void addIfPresent(final Optional<?> optional, final String key,
            final Map<String, Object> map) {
        if (optional.isPresent()) {
            map.put(key, optional.get());
        }
    }

    /**
     * Format an array of HttpClient header objects.
     *
     * @param headers
     *            the headers to format.
     * @return an array containing the string representation of each
     *         corresponding header.
     */
    static String[] formatHeaders(final Header[] headers) {
        final Comparator<Header> cmp = (a, b) -> {
            return b.getName().compareTo(b.getName());
        };
        return Arrays.stream(headers).filter(x -> x != null)
                .sorted(cmp)
                .map(h -> h.getName() + "=" + h.getValue())
                .collect(Collectors.toList())
                .toArray(new String[]{});
    }

    /**
     * Extract the value of a header.
     *
     * @param h
     *            the header.
     * @return the value of the header, or an empty optional if the header is
     *         {@code null}.
     */
    static Optional<String> headerValue(final Header h) {
        if (h == null) {
            return Optional.empty();
        }
        return Optional.of(h.getValue());
    }


    /** No instances allowed. */
    private Status() { }
}
