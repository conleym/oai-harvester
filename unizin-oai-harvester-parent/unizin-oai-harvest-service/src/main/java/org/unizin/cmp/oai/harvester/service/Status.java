package org.unizin.cmp.oai.harvester.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
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
import org.slf4j.Logger;
import org.unizin.cmp.oai.ResumptionToken;
import org.unizin.cmp.oai.harvester.exception.HarvesterHTTPStatusException;


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

    static Optional<String> formatStackTrace(
            final Optional<Exception> optional) {
        return withOptional(optional, ex -> {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            return sw.toString();
        });
    }

    static String formatInstant(final Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    static Optional<String> formatInstant(final Optional<Instant> optional) {
        return withOptional(optional, instant -> formatInstant(instant));
    }

    static Optional<String> formatURI(final Optional<URI> optional) {
        return withOptional(optional, uri -> uri.toString());
    }

    static <T,U> Optional<String> formatMap(
            final Optional<? extends Map<T,U>> optional) {
        return withOptional(optional, map -> map.toString());
    }

    static Optional<Map<String, Object>> convertResumptionToken(
            final Optional<ResumptionToken> optional) {
        return withOptional(optional, token -> {
                    final Map<String, Object> m = token.toMap();
                    final Optional<Instant> exp = token.getExpirationDate();
                    addIfPresent(formatInstant(exp), "expirationDate", m);
                    return m;
                });
    }

    static void addIfPresent(final Optional<?> optional, final String key,
            final Map<String, Object> map) {
        if (optional.isPresent()) {
            map.put(key, optional.get());
        }
    }

    static InputStream responseBody(final HarvesterHTTPStatusException ex,
            final Logger logger) {
        final PipedInputStream pis = new PipedInputStream();
        new Thread(() -> {
            try (final PipedOutputStream out = new PipedOutputStream(pis)) {
                ex.writeResponseBodyTo(out);
            } catch (final IOException e) {
                logger.error("Error writing response body to database.", e);
            }
        });
        return pis;
    }

    static String[] formatHeaders(final Header[] headers) {
        final Comparator<Header> cmp = (a, b) -> {
            return b.getName().compareTo(b.getName());
        };
        return Arrays.stream(headers)
                .sorted(cmp)
                .map(h -> h.getName() + "=" + h.getValue())
                .collect(Collectors.toList())
                .toArray(new String[]{});
    }


    /** No instances allowed. */
    private Status() { }
}
