package org.unizin.cmp.oai.harvester.service;

import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.skife.jdbi.v2.HashPrefixStatementRewriter;
import org.skife.jdbi.v2.ResultSetMapperFactory;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.exceptions.ResultSetException;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.OverrideStatementRewriterWith;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.io.CharStreams;

@OverrideStatementRewriterWith(HashPrefixStatementRewriter.class)
public interface JobJDBI extends AutoCloseable {

    public static final class Mapper
        implements ResultSetMapper<Map<String, Object>> {

        private static final String toUpper(final String str) {
            return str == null ? null : str.toUpperCase();
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
                        try (final Reader reader = ((Clob)value).getCharacterStream()) {
                            value = CharStreams.toString(reader);
                        } catch (final IOException e) {
                            throw new ResultSetException("Error reading clob",
                                    e, ctx);
                        }
                    }
                    row.put(alias != null ? alias : key, value);
                }
            }
            catch (SQLException e) {
                throw new ResultSetException(
                        "Unable to access specific metadata from " +
                                "result set metadata", e, ctx);
            }
            return row;
        }
    }

    @SuppressWarnings("rawtypes")
    public static final class MapperFactory implements ResultSetMapperFactory {
        private static final ResultSetMapper MAPPER = new Mapper();
        @Override
        public boolean accepts(Class type, StatementContext ctx) {
            return Map.class.isAssignableFrom(type);
        }

        @Override
        public ResultSetMapper mapperFor(Class type, StatementContext ctx) {
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

    public static final String INSERT_HARVEST = "insert into HARVEST(" +
            "JOB_ID, REPOSITORY_ID, HARVEST_INITIAL_PARAMETERS) " +
            "values (#jobID, #repoID, #initialParameters)";

    @SqlUpdate("insert into JOB() values()")
    @GetGeneratedKeys
    long createJob();

    @SqlUpdate(JOB_UPDATE)
    void updateJob(@Bind("id") long id, @Bind("start") String start,
            @Bind("end") String end, @Bind("stackTrace") String stackTrace,
            @Bind("recordsReceived") long recordsReceived,
            @Bind("recordBytesReceived") long recordBytesReceived,
            @Bind("batchesAttempted") long batchesAttempted);

    @SqlUpdate(INSERT_HARVEST)
    @GetGeneratedKeys
    long createHarvest(@Bind("jobID") long jobID,
            @Bind("repoID") long repositoryID,
            @Bind("initialParameters") String initialParameters);

    @SqlUpdate(HARVEST_UPDATE)
    void updateHarvest(@Bind("id") long id, @Bind("start") String start,
            @Bind("end") String end,
            @Bind("initialParameters") String initialParameters,
            @Bind("cancelled") boolean cancelled,
            @Bind("interrupted") boolean interrupted,
            @Bind("lastRequestURI") String lastRequestURI,
            @Bind("lastRequestParameters") String lastRequestParameters,
            @Bind("stackTrace") String stackTrace,
            @Bind("requestCount") long requestCount,
            @Bind("responseCount") long responseCount,
            @Bind("eventCount") long eventCount);

    @SqlQuery("select REPOSITORY_ID from REPOSITORY " +
                 "where REPOSITORY_BASE_URI = #baseURI")
    long findRepositoryIDByBaseURI(
            @Bind("baseURI") String baseURI);

    @SqlQuery("select * from JOB inner join HARVEST " +
                    "on JOB.JOB_ID = HARVEST.JOB_ID " +
                 "where JOB.JOB_ID = #id")
    @SingleValueResult(Map.class)
    @RegisterMapperFactory(MapperFactory.class)
    List<Map<String, Object>> findJobByID(@Bind("id") long id);


    @Override
    void close();
}
