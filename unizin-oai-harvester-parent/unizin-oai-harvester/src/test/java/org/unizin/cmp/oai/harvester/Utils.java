package org.unizin.cmp.oai.harvester;

import java.io.ByteArrayInputStream;
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

    public static InputStream fromString(final String string) {
        return new ByteArrayInputStream(string.getBytes(
                StandardCharsets.UTF_8));
    }

    public static InputStream fromClasspathFile(final String filename) {
        final InputStream in = Utils.class.getResourceAsStream(filename);
        if (in == null) {
            throw new IllegalArgumentException("File " + filename +
                    " not found on the classpath.");
        }
        return in;
    }

    /** No instances allowed. */
    private Utils() {}
}
