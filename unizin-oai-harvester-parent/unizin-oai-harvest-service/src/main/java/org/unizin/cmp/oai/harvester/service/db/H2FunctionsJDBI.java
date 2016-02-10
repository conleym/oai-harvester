package org.unizin.cmp.oai.harvester.service.db;

import java.util.List;
import java.util.Map;

import org.skife.jdbi.v2.HashPrefixStatementRewriter;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.OverrideStatementRewriterWith;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.unizin.cmp.oai.OAIVerb;

/**
 * Database access used only to implement stored functions in H2.
 */
@OverrideStatementRewriterWith(HashPrefixStatementRewriter.class)
abstract class H2FunctionsJDBI {
    private static final String REPO_TMP_TBL = "NX_REPOSITORY";


    private static final String INSERT_HARVEST_PROTOCOL_ERROR = "insert into " +
            "HARVEST_PROTOCOL_ERROR(HARVEST_ID, " +
            "HARVEST_PROTOCOL_ERROR_MESSAGE, HARVEST_PROTOCOL_ERROR_CODE) " +
            "values (#id, #errorMessage, #errorCode)";
    @SqlUpdate(INSERT_HARVEST_PROTOCOL_ERROR)
    abstract void insertHarvestProtocolError(@Bind("id") long harvestID,
            @Bind("errorMessage") String errorMessage,
            @Bind("errorCode") String errorCode);


    private static final String INSERT_HARVEST = "insert into HARVEST(" +
            "JOB_ID, REPOSITORY_ID, HARVEST_INITIAL_PARAMETERS, " +
            " HARVEST_VERB) values (#jobID, #repoID, #initialParameters, " +
            "#verb)";
    @SqlUpdate(INSERT_HARVEST)
    @GetGeneratedKeys
    abstract long createHarvest(@Bind("jobID") long jobID,
            @Bind("repoID") long repositoryID,
            @Bind("initialParameters") String initialParameters,
            @Bind("verb") OAIVerb verb);


    private static final String PROTOCOL_ERROR_QUERY = "select " +
            "HARVEST_PROTOCOL_ERROR_MESSAGE, HARVEST_PROTOCOL_ERROR_CODE " +
            " from HARVEST_PROTOCOL_ERROR where HARVEST_ID = #id";
    @SqlQuery(PROTOCOL_ERROR_QUERY)
    @SingleValueResult(Map.class)
    @RegisterMapperFactory(CMPMapperFactory.class)
    public abstract List<Map<String, Object>> readOAIErrors(
            @Bind("id") long harvestID);


    @SqlQuery("select * from REPOSITORY where REPOSITORY_BASE_URI = #baseURI")
    @RegisterMapperFactory(CMPMapperFactory.class)
    public abstract Map<String, Object> findRepositoryIDByBaseURI(
            @Bind("baseURI") String baseURI);


    private static final String CREATE_REPOSITORY_TMP = "create local " +
            "temporary table if not exists " + REPO_TMP_TBL + "(" +
            "REPOSITORY_BASE_URI varchar(1024), " +
            "REPOSITORY_NAME varchar(1024), " +
            "REPOSITORY_INSTITUTION varchar(1024)" +
            ")";
    @SqlUpdate(CREATE_REPOSITORY_TMP)
    abstract void createReposTempTable();

    @SqlUpdate("delete from " + REPO_TMP_TBL)
    abstract void clearTempTable();

    private static final String REPOSITORY_TMP_INSERT = "insert into " +
            REPO_TMP_TBL + "(REPOSITORY_NAME, REPOSITORY_BASE_URI, " +
            "REPOSITORY_INSTITUTION) values (#name, #baseURI, #institution)";
    @SqlBatch(REPOSITORY_TMP_INSERT)
    @BatchChunkSize(1000)
    abstract void addReposToTempTable(@Bind("name") List<String> names,
            @Bind("baseURI") List<String> baseURIs,
            @Bind("institution") List<String> institutions);


    private static final String DISABLE_REPOS = "update REPOSITORY " +
            "set REPOSITORY_ENABLED = false where REPOSITORY_BASE_URI not in " +
            "(select REPOSITORY_BASE_URI from " + REPO_TMP_TBL + ")";
    @SqlUpdate(DISABLE_REPOS)
    abstract void disableReposNotInNuxeo();


    private static final String UPDATE_REPOS = "merge into REPOSITORY(" +
            "REPOSITORY_BASE_URI, REPOSITORY_NAME, REPOSITORY_INSTITUTION, " +
            "REPOSITORY_ENABLED) key (REPOSITORY_BASE_URI) " +
            "(select T.*, true as REPOSITORY_ENABLED from " + REPO_TMP_TBL +
            " T)";
    @SqlUpdate(UPDATE_REPOS)
    abstract void mergeNuxeoUpdates();
}
