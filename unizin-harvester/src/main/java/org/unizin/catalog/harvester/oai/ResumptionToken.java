package org.unizin.catalog.harvester.oai;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.time.LocalDateTime;

public class ResumptionToken {
    public final LocalDateTime expirationDate;
    public final int completeListSize;
    public final int cursor;
    public final String token;

    public ResumptionToken(LocalDateTime expirationDate, int completeListSize,
                           int cursor, String token) {
        this.expirationDate = expirationDate;
        this.completeListSize = completeListSize;
        this.cursor = cursor;
        this.token = token;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("expirationDate", expirationDate)
                .append("completeListSize", completeListSize)
                .append("cursor", cursor)
                .append("token", token)
                .toString();
    }
}
