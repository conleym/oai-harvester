package org.unizin.cmp.oai.harvester.service;

import java.util.Map;

import org.skife.jdbi.v2.HashPrefixStatementRewriter;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.OverrideStatementRewriterWith;

@OverrideStatementRewriterWith(HashPrefixStatementRewriter.class)
public interface JobJDBI extends AutoCloseable {
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

    @SqlQuery("select * from JOB where JOB_ID = #id")
    Map<String, Object> findJobByID(@Bind("id") long id);


    @Override
    void close();
}
