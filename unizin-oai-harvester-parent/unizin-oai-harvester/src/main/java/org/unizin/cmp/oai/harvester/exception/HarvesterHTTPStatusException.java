package org.unizin.cmp.oai.harvester.exception;

import java.io.IOException;
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
    private final Header[] headers;
    private final Locale locale;
    private final StatusLine statusLine;


    public HarvesterHTTPStatusException(final String message,
            final HttpResponse response) {
        super(message);
        this.response = response;
        this.headers = response.getAllHeaders();
        this.locale = response.getLocale();
        this.statusLine = response.getStatusLine();
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
        return headers;
    }

    public StatusLine getStatusLine() {
        return statusLine;
    }

    public Locale getLocale() {
        return locale;
    }
}
