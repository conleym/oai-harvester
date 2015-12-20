package org.unizin.cmp.oai;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;


/**
 * Enumeration of standard <a href=
 * "http://www.openarchives.org/OAI/openarchivesprotocol.html#Datestamp">
 * datestamp granularities</a> used to specify dates for selective harvesting.
 * <p>
 * All compliant repositories must support the {@link #DAY} granularity.
 * {@link #SECOND} is optional.
 * </p>
 */
public enum OAIDateGranularity {
    DAY(DateTimeFormatter.ISO_LOCAL_DATE),
    SECOND(new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendPattern("'T'hh:mm:ss'Z'")
            .toFormatter());

    private final DateTimeFormatter formatter;

    private OAIDateGranularity(final DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    public TemporalAccessor parse(final String string) {
        return formatter.parse(string);
    }

    public String format(final TemporalAccessor ta) {
        return formatter.format(ta);
    }

    /**
     * Get an instance from a format string.
     * <p>
     * The format string should be the value of the {@code granularity} from a
     * repository's <a href=
     * "http://www.openarchives.org/OAI/openarchivesprotocol.html#Identify">
     * Identify response</a>.
     *
     * @param format
     *            the format string.
     * @return the corresponding granularity, or {@code null} if the format is
     *         not recognized.
     */
    public static OAIDateGranularity fromFormat(final String format) {
        switch(format) {
        case "YYYY-MM-DD":
            return DAY;
        case "YYYY-MM-DDThh:mm:ssZ":
        default:
            return null;
        }
    }
}
