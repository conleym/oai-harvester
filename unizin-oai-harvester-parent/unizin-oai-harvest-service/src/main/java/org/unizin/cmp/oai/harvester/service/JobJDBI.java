package org.unizin.cmp.oai.harvester.service;

import static org.unizin.cmp.oai.harvester.service.Status.formatHeaders;
import static org.unizin.cmp.oai.harvester.service.Status.headerValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.skife.jdbi.v2.DefaultMapper;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.HashPrefixStatementRewriter;
import org.skife.jdbi.v2.ResultSetMapperFactory;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.exceptions.ResultSetException;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.Transaction;
import org.skife.jdbi.v2.sqlobject.customizers.OverrideStatementRewriterWith;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.skife.jdbi.v2.sqlobject.mixins.GetHandle;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestStatistic;
import org.unizin.cmp.oai.harvester.exception.HarvesterHTTPStatusException;
import org.unizin.cmp.oai.harvester.exception.OAIProtocolException;

import com.google.common.io.CharStreams;

/**
* JDBI database access methods.
*/
/*
 * Hash prefix should make converting to PostgreSQL easier, should it ever be
 * necessary.
 */
@OverrideStatementRewriterWith(HashPrefixStatementRewriter.class)
public abstract class JobJDBI implements AutoCloseable, GetHandle {
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
    public static final class Mapper
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

    /** A factory that produces {@link Mapper} instances. */
    @SuppressWarnings("rawtypes") // Alas, the interface specifies them.
    public static final class MapperFactory implements ResultSetMapperFactory {
        DefaultMapper dm;
        private static final ResultSetMapper MAPPER = new Mapper();
        @Override
        public boolean accepts(final Class type, final StatementContext ctx) {
            return Map.class.isAssignableFrom(type);
        }

        @Override
        public ResultSetMapper mapperFor(final Class type,
                final StatementContext ctx) {
            return MAPPER ;
        }
    }


    private static final String JOB_UPDATE = "update JOB " +
            "set JOB_START = #start," +
            "JOB_END = #end, " +
            "JOB_LAST_UPDATE = now(), " +
            "JOB_RECORDS_RECEIVED = #recordsReceived, " +
            "JOB_RECORD_BYTES_RECEIVED = #recordBytesReceived, " +
            "JOB_BATCHES_ATTEMPTED = #batchesAttempted " +
            "where JOB_ID = #id";

    private static final String HARVEST_UPDATE = "update HARVEST " +
            "set HARVEST_START = #start, " +
            "HARVEST_END = #end, " +
            "HARVEST_LAST_UPDATE = now(), " +
            "HARVEST_CANCELLED = #cancelled, " +
            "HARVEST_INTERRUPTED = #interrupted, " +
            "HARVEST_LAST_REQUEST_URI = #lastRequestURI," +
            "HARVEST_LAST_REQUEST_PARAMETERS = #lastRequestParameters, " +
            "HARVEST_STACK_TRACE = #stackTrace, " +
            "HARVEST_REQUEST_COUNT = #requestCount, " +
            "HARVEST_RESPONSE_COUNT = #responseCount, " +
            "HARVEST_XML_EVENT_COUNT = #eventCount " +
            "where HARVEST_ID = #id";

    private static final String INSERT_HARVEST_HTTP_ERROR = "insert into " +
            "HARVEST_HTTP_ERROR(HARVEST_ID, HARVEST_HTTP_ERROR_STATUS_CODE, " +
            "HARVEST_HTTP_ERROR_RESPONSE_BODY, " +
            "HARVEST_HTTP_ERROR_CONTENT_ENCODING, " +
            "HARVEST_HTTP_ERROR_CONTENT_TYPE, HARVEST_HTTP_ERROR_HEADERS) " +
            "values (#id, #statusCode, #responseBody, #contentEncoding, " +
            "#contentType, #headers)";

    private static final String INSERT_HARVEST_PROTOCOL_ERROR = "insert into " +
            "HARVEST_PROTOCOL_ERROR(HARVEST_ID, " +
            "HARVEST_PROTOCOL_ERROR_MESSAGE, HARVEST_PROTOCOL_ERROR_CODE) " +
            "values (#id, #errorMessage, #errorCode)";

    private static final String PROTOCOL_ERROR_QUERY = "select " +
            "HARVEST_PROTOCOL_ERROR_MESSAGE, HARVEST_PROTOCOL_ERROR_CODE " +
            " from HARVEST_PROTOCOL_ERROR where HARVEST_ID = #id";

    private static final String INSERT_HARVEST = "insert into HARVEST(" +
            "JOB_ID, REPOSITORY_ID, HARVEST_INITIAL_PARAMETERS, " +
            " HARVEST_VERB) values (#jobID, #repoID, #initialParameters, " +
            "#verb)";

    private static final String JOB_QUERY = "select J.*, H.*, R.*, E.*, "
            + "OAI_ERRORS(H.HARVEST_ID) as HARVEST_PROTOCOL_ERRORS from " +
            "JOB J inner join HARVEST H on J.JOB_ID = H.JOB_ID " +
            "inner join REPOSITORY R on R.REPOSITORY_ID = H.REPOSITORY_ID " +
            "left outer join HARVEST_HTTP_ERROR E " +
            "on H.HARVEST_ID = E.HARVEST_ID " +
            "left outer join HARVEST_PROTOCOL_ERROR P " +
            "on H.HARVEST_ID = P.HARVEST_ID " +
            "where J.JOB_ID = #id";

    @SqlUpdate("insert into JOB() values()")
    @GetGeneratedKeys
    public abstract long createJob();

    @SqlUpdate(JOB_UPDATE)
    public abstract void updateJob(@Bind("id") long id,
            @Bind("start") Instant start,
            @Bind("end") Optional<Instant> end,
            @Bind("stackTrace") Optional<String> stackTrace,
            @Bind("recordsReceived") long recordsReceived,
            @Bind("recordBytesReceived") long recordBytesReceived,
            @Bind("batchesAttempted") long batchesAttempted);

    @SqlUpdate(INSERT_HARVEST)
    @GetGeneratedKeys
    public abstract long createHarvest(@Bind("jobID") long jobID,
            @Bind("repoID") long repositoryID,
            @Bind("initialParameters") String initialParameters,
            @Bind("verb") OAIVerb verb);

    @SqlUpdate(HARVEST_UPDATE)
    public abstract void updateHarvest(@Bind("id") long id,
            @Bind("start") Instant start,
            @Bind("end") Optional<Instant> end,
            @Bind("cancelled") boolean cancelled,
            @Bind("interrupted") boolean interrupted,
            @Bind("lastRequestURI") Optional<String> lastRequestURI,
            @Bind("lastRequestParameters")
    Optional<String> lastRequestParameters,
    @Bind("stackTrace") Optional<String> stackTrace,
    @Bind("requestCount") long requestCount,
    @Bind("responseCount") long responseCount,
    @Bind("eventCount") long eventCount);

    @SqlUpdate(INSERT_HARVEST_HTTP_ERROR)
    public abstract void insertHarvestHTTPError(@Bind("id") long harvestID,
            @Bind("statusCode") int statusCode,
            @Bind("headers") String[] headers,
            @Bind("contentType") Optional<String> contentType,
            @Bind("contentEncoding") Optional<String> contentEncoding,
            @Bind("responseBody") InputStream body);

    @SqlUpdate(INSERT_HARVEST_PROTOCOL_ERROR)
    public abstract void insertHarvestProtocolError(@Bind("id") long harvestID,
            @Bind("errorMessage") String errorMessage,
            @Bind("errorCode") String errorCode);

    @SqlQuery("select REPOSITORY_ID from REPOSITORY " +
            "where REPOSITORY_BASE_URI = #baseURI")
    public abstract long findRepositoryIDByBaseURI(
            @Bind("baseURI") String baseURI);

    @SqlQuery(JOB_QUERY)
    @SingleValueResult(Map.class)
    @RegisterMapperFactory(MapperFactory.class)
    public abstract List<Map<String, Object>> findJobByID(@Bind("id") long id);

    @SqlQuery(PROTOCOL_ERROR_QUERY)
    @SingleValueResult(Map.class)
    @RegisterMapperFactory(MapperFactory.class)
    public abstract List<Map<String, Object>> readOAIErrors(
            @Bind("id") long harvestID);

    @Override
    public abstract void close();

    /**
     * Ensure that exceptions are written only once, as there's no facility
     * within the database or the application to constrain duplicate entries.
     *
     * @param notification
     *            the notification to check.
     * @return {@code true} iff the notification has an exception and is
     *         reporting the end of the harvest.
     */
    private static boolean writeExceptionInfo(
            final HarvestNotification notification) {
        return notification.getType() == HarvestNotificationType.HARVEST_ENDED
                && notification.getException().isPresent();
    }

    private static InputStream responseBody(
            final HarvesterHTTPStatusException ex, final Logger logger) {
        final PipedInputStream pis = new PipedInputStream();
        final PipedOutputStream pos = new PipedOutputStream();
        new Thread(() -> {
            try (final OutputStream out = pos) {
                pos.connect(pis);
                ex.writeResponseBodyTo(out);
            } catch (final IOException e) {
                logger.error("Error writing response body to database.", e);
            }
        }).start();
        return pis;
    }

    @Transaction
    public void harvestDatabaseUpdate(final long harvestID,
            final Optional<String> lastRequestURI,
            final Optional<String> lastRequestParameters,
            final Optional<String> stackTrace,
            final HarvestNotification notification,
            final Logger logger) {
        updateHarvest(harvestID, notification.getStarted(),
                notification.getEnded(),
                notification.isCancelled(),
                notification.isInterrupted(),
                lastRequestURI,
                lastRequestParameters,
                stackTrace,
                notification.getStat(HarvestStatistic.REQUEST_COUNT),
                notification.getStat(HarvestStatistic.RESPONSE_COUNT),
                notification.getStat(HarvestStatistic.XML_EVENT_COUNT));
        if (writeExceptionInfo(notification)) {
            final Exception ex = notification.getException().get();
            if (ex instanceof HarvesterHTTPStatusException) {
                final HarvesterHTTPStatusException hhse =
                        (HarvesterHTTPStatusException)ex;
                try (final InputStream in = responseBody(hhse, logger)) {
                    insertHarvestHTTPError(harvestID,
                        hhse.getStatusLine().getStatusCode(),
                        formatHeaders(hhse.getHeaders()),
                        headerValue(hhse.getContentType()),
                        headerValue(hhse.getContentEncoding()),
                        in);
                } catch (final IOException e) {
                    logger.warn(
                            "Error closing HTTP error response body stream.",
                            e);
                }
            } else if (ex instanceof OAIProtocolException) {
                final OAIProtocolException ope = (OAIProtocolException)ex;
                final Handle h = getHandle();
                h.setStatementRewriter(new HashPrefixStatementRewriter());
                h.createCall("call INSERT_OAI_ERRORS(#id, #errors)")
                    .bind("id", harvestID)
                    .bind("errors", ope.getOAIErrors())
                    .invoke();
            }
        }
    }
}
