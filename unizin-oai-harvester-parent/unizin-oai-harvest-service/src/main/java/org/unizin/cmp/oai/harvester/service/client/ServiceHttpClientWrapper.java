package org.unizin.cmp.oai.harvester.service.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;

import com.google.common.io.ByteStreams;

final class ServiceHttpClientWrapper {
    @FunctionalInterface
    static interface BadStatusCodeHandler<T extends RuntimeException> {
        T apply(int statusCode, String responseBody);
    }

    @FunctionalInterface
    static interface NullEntityHandler<T extends RuntimeException>
        extends Supplier<T> { }

    @FunctionalInterface
    static interface IOExceptionHandler<T extends RuntimeException>
        extends Function<IOException, T> { }


    private final Logger logger;
    private final BasicAuthHttpClient basicAuthHttpClient;
    private final Collection<Integer> acceptedStatusCodes;
    private final BadStatusCodeHandler<?> badStatusCodeHandler;
    private final NullEntityHandler<?> nullEntityHandler;
    private final IOExceptionHandler<?> ioExceptionHandler;


    ServiceHttpClientWrapper(final Logger logger,
            final BasicAuthHttpClient basicAuthHttpClient,
            final Collection<Integer> acceptedStatusCodes,
            final BadStatusCodeHandler<?> badStatusCodeHandler,
            final NullEntityHandler<?> nullEntityHandler,
            final IOExceptionHandler<?> ioExceptionHandler) {
        this.logger = logger;
        this.basicAuthHttpClient = basicAuthHttpClient;
        this.acceptedStatusCodes = acceptedStatusCodes;
        this.badStatusCodeHandler = badStatusCodeHandler;
        this.nullEntityHandler = nullEntityHandler;
        this.ioExceptionHandler = ioExceptionHandler;
    }

    protected final Charset contentEncoding(final HttpEntity entity) {
        final Header contentEncoding = entity.getContentEncoding();
        if (contentEncoding != null) {
            return Charset.forName(contentEncoding.getValue());
        }
        final Header contentType = entity.getContentType();
        if (contentType != null) {
            try {
                final ContentType type = ContentType.parse(
                        contentType.getValue());
                final Charset charset = type.getCharset();
                if (charset != null) {
                    return charset;
                }
            } catch (final ParseException | UnsupportedCharsetException e) {
                logger.warn("Error parsing content type header.", e);
            }
        }
        return StandardCharsets.ISO_8859_1;
    }

    private final String responseBody(final HttpResponse response) {
        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            return "";
        }
        try (final InputStream in = entity.getContent()) {
            final Charset encoding = contentEncoding(entity);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteStreams.copy(in, baos);
            return new String(baos.toByteArray(), encoding);
        } catch (final IOException e) {
            logger.warn("Error reading error response body.", e);
            return null;
        }
    }

    HttpResponse execute(final HttpUriRequest request) {
        try {
            logger.trace("Sending request {}", request);
            final HttpResponse response = basicAuthHttpClient.execute(request);
            logger.debug("Got response {} to request {}", response, request);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (!acceptedStatusCodes.contains(statusCode)) {
                throw badStatusCodeHandler.apply(statusCode,
                        responseBody(response));
            }
            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw nullEntityHandler.get();
            }
            return response;
        } catch (final IOException e) {
            throw ioExceptionHandler.apply(e);
        }
    }
}
