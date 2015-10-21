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
