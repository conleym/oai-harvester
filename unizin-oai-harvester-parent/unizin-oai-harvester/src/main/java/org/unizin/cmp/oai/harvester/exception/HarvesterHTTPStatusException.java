package org.unizin.cmp.oai.harvester.exception;

import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.StatusLine;

public final class HarvesterHTTPStatusException extends HarvesterException {
    private static final long serialVersionUID = 1L;

    private final StatusLine statusLine;
    private final Locale locale;
    private final Header[] headers;
    private final byte[] responseBody;

    public HarvesterHTTPStatusException(final String message,
            final StatusLine statusLine, final Locale locale,
            final Header[] headers, final byte[] responseBody) {
        super(message);
        this.statusLine = statusLine;
        this.locale = locale;
        this.headers = headers;
        this.responseBody = responseBody;
    }

    public byte[] getResponseBody() {
        return responseBody;
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
