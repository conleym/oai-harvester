package org.unizin.cmp.oai.harvester;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.google.common.io.ByteStreams;

public final class Utils {
	
	public static String fromStream(final InputStream in) throws IOException {
		try (final InputStream is = in) {
			final byte[] bytes = ByteStreams.toByteArray(is);
			return new String(bytes, StandardCharsets.UTF_8);
		} 
	}
	
	

	/** No instances allowed. */
	private Utils() {}
}
