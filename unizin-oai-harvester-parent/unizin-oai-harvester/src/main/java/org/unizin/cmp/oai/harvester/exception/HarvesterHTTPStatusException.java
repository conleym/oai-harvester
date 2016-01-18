package org.unizin.cmp.oai.harvester.exception;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

/**
 * Exception thrown when a repository returns an HTTP status code other than 200
 * OK.
 * <p>
 * Instances are serializable, but note that serialized instances will not
 * contain the response body from the server. Other information, like headers
 * and the status line, are preserved.
 * </p>
 */
public final class HarvesterHTTPStatusException extends HarvesterException {
    private static final long serialVersionUID = 1L;

    // HttpResponse is not serializable.
    private transient final HttpResponse response;

    // These are provided to enable serialization and deserialization.
    private Header[] headers;
    private Locale locale;
    private StatusLine statusLine;


    public HarvesterHTTPStatusException(final String message,
            final HttpResponse response) {
        super(message);
        this.response = response;
    }

    public void writeResponseBodyTo(final OutputStream out) throws IOException {
        if (response != null) {
            final HttpEntity e = response.getEntity();
            if (e != null) {
                e.writeTo(out);
            }
        }
    }

    public Header[] getHeaders() {
        if (response != null) {
            return response.getAllHeaders();
        }
        return headers;
    }

    public StatusLine getStatusLine() {
        if (response != null) {
            return response.getStatusLine();
        }
        return statusLine;
    }

    public Locale getLocale() {
        if (response != null) {
            return response.getLocale();
        }
        return locale;
    }

    private void writeObject(final ObjectOutputStream oos) throws IOException {
        /* HttpResponse cannot be serialized, but most of its contents can.
         * Before serializing, we grab our own reference to the serializable
         * things so they can be restored.
         *
         * In the process of serialization, access to the response body is
         * lost. The alternative is to read the body into memory before writing
         * it out, but, since we can't really be sure how much memory that might
         * take, it seems like a bad idea.
         */
        if (response != null) {
            headers = response.getAllHeaders();
            statusLine = response.getStatusLine();
            locale = response.getLocale();
        }
        oos.defaultWriteObject();
    }
}
