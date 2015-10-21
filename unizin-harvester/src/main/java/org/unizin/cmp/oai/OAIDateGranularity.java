package org.unizin.cmp.oai;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;

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
}
