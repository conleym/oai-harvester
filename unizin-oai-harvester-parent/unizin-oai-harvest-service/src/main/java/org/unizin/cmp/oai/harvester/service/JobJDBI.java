package org.unizin.cmp.oai.harvester.service;

import static org.unizin.cmp.oai.harvester.service.Status.formatHeaders;
import static org.unizin.cmp.oai.harvester.service.Status.responseBody;

import java.io.IOException;
import java.io.InputStream;
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

import org.apache.http.Header;
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

@OverrideStatementRewriterWith(HashPrefixStatementRewriter.class)
public abstract class JobJDBI implements AutoCloseable, GetHandle {

    public static final class Mapper
    implements ResultSetMapper<Map<String, Object>> {

        private static final String toUpper(final String str) {
            return str == null ? null : str.toUpperCase();
        }

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
                    row.put(alias != null ? alias : key, value);
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

    @SuppressWarnings("rawtypes")
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


    public static final String JOB_UPDATE = "update JOB " +
            "set JOB_START = #start," +
            "JOB_END = #end, " +
            "JOB_LAST_UPDATE = now(), " +
            "JOB_RECORDS_RECEIVED = #recordsReceived, " +
            "JOB_RECORD_BYTES_RECEIVED = #recordBytesReceived, " +
            "JOB_BATCHES_ATTEMPTED = #batchesAttempted " +
            "where JOB_ID = #id";

    public static final String HARVEST_UPDATE = "update HARVEST " +
            "set HARVEST_START = #start, " +
            "HARVEST_END = #end, " +
            "HARVEST_LAST_UPDATE = now(), " +
            "HARVEST_INITIAL_PARAMETERS = #initialParameters, " +
            "HARVEST_CANCELLED = #cancelled, " +
            "HARVEST_INTERRUPTED = #interrupted, " +
            "HARVEST_LAST_REQUEST_URI = #lastRequestURI," +
            "HARVEST_LAST_REQUEST_PARAMETERS = #lastRequestParameters, " +
            "HARVEST_STACK_TRACE = #stackTrace, " +
            "HARVEST_REQUEST_COUNT = #requestCount, " +
            "HARVEST_RESPONSE_COUNT = #responseCount, " +
            "HARVEST_XML_EVENT_COUNT = #eventCount " +
            "where HARVEST_ID = #id";

    public static final String INSERT_HARVEST_HTTP_ERROR = "insert into " +
            "HARVEST_HTTP_ERROR(HARVEST_ID, HARVEST_HTTP_ERROR_STATUS_CODE, " +
            "HARVEST_HTTP_ERROR_RESPONSE_BODY, " +
            "HARVEST_HTTP_ERROR_CONTENT_ENCODING, " +
            "HARVEST_HTTP_ERROR_CONTENT_TYPE, HARVEST_HTTP_ERROR_HEADERS) " +
            "values (#jobID, #statusCode, #responseBody, #contentEncoding, " +
            "#contentType, #headers)";

    public static final String INSERT_HARVEST_PROTOCOL_ERROR = "insert into " +
            "HARVEST_PROTOCOL_ERROR(HARVEST_ID, " +
            "HARVEST_PROTOCOL_ERROR_MESSAGE, HARVEST_PROTOCOL_ERROR_CODE) " +
            "values (#id, #errorMessage, #errorCode)";

    public static final String INSERT_HARVEST = "insert into HARVEST(" +
            "JOB_ID, REPOSITORY_ID, HARVEST_INITIAL_PARAMETERS, " +
            " HARVEST_VERB) values (#jobID, #repoID, #initialParameters, " +
            "#verb)";

    public static final String JOB_QUERY = "select * from JOB inner join " +
            "HARVEST on JOB.JOB_ID = HARVEST.JOB_ID inner join REPOSITORY on " +
            "REPOSITORY.REPOSITORY_ID = HARVEST.REPOSITORY_ID " +
            "where JOB.JOB_ID = #id";

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
            @Bind("initialParameters") String initialParameters,
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

    @Override
    public abstract void close();

    private static boolean writeExceptionInfo(
            final HarvestNotification notification) {
        return notification.getType() == HarvestNotificationType.HARVEST_ENDED
                && notification.getException().isPresent();
    }

    private static Optional<String> headerValue(final Header h) {
        if (h == null) {
            return Optional.empty();
        }
        return Optional.of(h.getValue());
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
                notification.getHarvestParameters().toString(),
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
                insertHarvestHTTPError(harvestID,
                        formatHeaders(hhse.getHeaders()),
                        headerValue(hhse.getContentType()),
                        headerValue(hhse.getContentEncoding()),
                        responseBody(hhse, logger));
            } else if (ex instanceof OAIProtocolException) {
                final OAIProtocolException ope = (OAIProtocolException)ex;
                final Handle h = getHandle();
                h.createCall("call INSERT_OAI_ERRORS(:id, :errors)")
                    .bind("id", harvestID)
                    .bind("errors", ope.getOAIErrors())
                    .invoke();
            }
        }
    }
}
