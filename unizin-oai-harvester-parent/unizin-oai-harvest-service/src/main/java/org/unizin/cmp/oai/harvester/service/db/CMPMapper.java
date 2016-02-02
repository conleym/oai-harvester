package org.unizin.cmp.oai.harvester.service.db;

import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.exceptions.ResultSetException;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.io.CharStreams;

/**
 * A minimal JDBI result set mapper.
 * <p>
 * This mapper differs from {@link org.skife.jdbi.v2.DefaultMapper} in the
 * following ways:
 * </p>
 * <ul>
 * <li>Jackson can serialize the maps produced by this mapper, whereas it
 * seems unable to serialize those produced by the default mapper.</li>
 * <li>The names of columns in the maps are capitalized here, whereas
 * they're lower case in the default (this is just my personal preference to
 * make field names more noticeable in the code).</li>
 * <li>Timestamps are converted to Java 8 instants.</li>
 * <li>Clobs are converted to strings.</li>
 * <li>Outer joins are handled correctly -- the default mapper can overwrite
 * a non-null value in the resulting map with null in some cases.</li>
 * </ul>
 */
public final class CMPMapper
implements ResultSetMapper<Map<String, Object>> {
    private static final String toUpper(final String str) {
        return str == null ? null : str.toUpperCase();
    }

    /**
     * Convert a clob to a string.
     * <p>
     * We use clobs for a variety of fields that could be somewhat large,
     * but none so large that reading them into memory could cause problems.
     * It makes sense, therefore, to simplify the database interface by
     * converting them to strings.
     * </p>
     *
     * @param clob
     *            the clob to convert.
     * @param ctx
     *            the JDBI context.
     * @return the contents of the given clob as a string.
     * @throws SQLException
     *             if there's an error reading the clob from the database.
     */
    private static final String fromClob(final Clob clob,
            final StatementContext ctx) throws SQLException {
        try (final Reader reader = clob.getCharacterStream()) {
            return CharStreams.toString(reader);
        } catch (final IOException e) {
            throw new ResultSetException("Error reading clob", e, ctx);
        }
    }

    @Override
    public Map<String, Object> map(final int index, final ResultSet r,
            final StatementContext ctx) {
        Map<String, Object> row = new HashMap<>();
        ResultSetMetaData m;
        try {
            m = r.getMetaData();
        }
        catch (SQLException e) {
            throw new ResultSetException(
                    "Unable to obtain metadata from result set", e, ctx);
        }
        try {
            for (int i = 1; i <= m.getColumnCount(); i ++) {
                String key = toUpper(m.getColumnName(i));
                String alias = toUpper(m.getColumnLabel(i));
                Object value = r.getObject(i);
                if (value instanceof Clob) {
                    value = fromClob((Clob)value, ctx);
                } else if (value instanceof Timestamp) {
                    value = ((Timestamp)value).toInstant();
                }
                final String s = alias == null ? key : alias;
                /*
                 * Use putIfAbsent to make outer joins sensible. Otherwise
                 * null values of columns used to join can show up in
                 * results.
                 */
                row.putIfAbsent(s, value);
            }
        }
        catch (final SQLException e) {
            throw new ResultSetException(
                    "Unable to access specific metadata from " +
                            "result set metadata", e, ctx);
        }
        return row;
    }
}