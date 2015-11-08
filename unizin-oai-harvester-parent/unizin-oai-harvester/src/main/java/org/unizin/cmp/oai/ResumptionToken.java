package org.unizin.cmp.oai;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Container for information supplied as part of a <a href=
 * "http://www.openarchives.org/OAI/openarchivesprotocol.html#FlowControl">
 * resumption token</a> in an incomplete list response.
 *
 */
public final class ResumptionToken {
    private final String token;
    private final Optional<Long> completeListSize;
    private final Optional<Long> cursor;
    private final Optional<Instant> expirationDate;


    /**
     * Create a new instance.
     *
     * @param token
     *            the resumption token (i.e., the content of all text nodes
     *            which are children of the {@code resumptionToken} element in
     *            the incomplete list response).
     *
     * @throws NullPointerException
     *             if {@code token} is {@code null}.
     */
    public ResumptionToken(final String token) {
        this(token, null, null, null);
    }

    /**
     * Create a new instance.
     *
     * @param token
     *            the resumption token (i.e., the content of all text nodes
     *            which are children of the {@code resumptionToken} element in
     *            the incomplete list response).
     * @param completeListSize
     *            the value of the {@code completeListSize} attribute on the
     *            {@code resumptionToken} element in the incomplete list
     *            response.
     * @param cursor
     *            the value of the {@code cursor} attribute on the
     *            {@code resumptionToken} element in the incomplete list
     *            response.
     * @param expirationDate
     *            the value of the {@code expirationDate} attribute on the
     *            {@code resumptionToken} element in the incomplete list
     *            response.
     * @throws NullPointerException
     *             if {@code token} is {@code null}.
     */
    public ResumptionToken(final String token,
            final Long completeListSize, final Long cursor,
            final Instant expirationDate) {
        Objects.requireNonNull(token, "token");
        this.token = token;
        this.completeListSize = Optional.ofNullable(completeListSize);
        this.cursor = Optional.ofNullable(cursor);
        this.expirationDate = Optional.ofNullable(expirationDate);
    }

    public String getToken() {
        return token;
    }

    public Optional<Long> getCursor() {
        return cursor;
    }

    public Optional<Long> getCompleteListSize() {
        return completeListSize;
    }

    public Optional<Instant> getExpirationDate() {
        return expirationDate;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((completeListSize == null) ? 0 : completeListSize.hashCode());
        result = prime * result + ((cursor == null) ? 0 : cursor.hashCode());
        result = prime * result + ((expirationDate == null) ? 0 : expirationDate.hashCode());
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResumptionToken other = (ResumptionToken) obj;
        if (completeListSize == null) {
            if (other.completeListSize != null)
                return false;
        } else if (!completeListSize.equals(other.completeListSize))
            return false;
        if (cursor == null) {
            if (other.cursor != null)
                return false;
        } else if (!cursor.equals(other.cursor))
            return false;
        if (expirationDate == null) {
            if (other.expirationDate != null)
                return false;
        } else if (!expirationDate.equals(other.expirationDate))
            return false;
        if (token == null) {
            if (other.token != null)
                return false;
        } else if (!token.equals(other.token))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(this.getClass().getName())
                .append("[token=")
                .append(token)
                .append(", cursor=")
                .append(cursor)
                .append(", completeListSize=")
                .append(completeListSize)
                .append(", expirationDate=")
                .append(expirationDate)
                .append("]")
                .toString();
    }
}
