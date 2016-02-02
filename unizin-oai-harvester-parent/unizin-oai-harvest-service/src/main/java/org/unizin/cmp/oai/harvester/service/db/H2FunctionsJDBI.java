package org.unizin.cmp.oai.harvester.service.db;

import java.util.List;
import java.util.Map;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.unizin.cmp.oai.OAIVerb;

/**
 * Database access used only to implement stored functions in H2.
 */
abstract class H2FunctionsJDBI {
    private static final String INSERT_HARVEST_PROTOCOL_ERROR = "insert into " +
            "HARVEST_PROTOCOL_ERROR(HARVEST_ID, " +
            "HARVEST_PROTOCOL_ERROR_MESSAGE, HARVEST_PROTOCOL_ERROR_CODE) " +
            "values (#id, #errorMessage, #errorCode)";

    @SqlUpdate(INSERT_HARVEST_PROTOCOL_ERROR)
    public abstract void insertHarvestProtocolError(@Bind("id") long harvestID,
            @Bind("errorMessage") String errorMessage,
            @Bind("errorCode") String errorCode);


    private static final String INSERT_HARVEST = "insert into HARVEST(" +
            "JOB_ID, REPOSITORY_ID, HARVEST_INITIAL_PARAMETERS, " +
            " HARVEST_VERB) values (#jobID, #repoID, #initialParameters, " +
            "#verb)";

    @SqlUpdate(INSERT_HARVEST)
    @GetGeneratedKeys
    public abstract long createHarvest(@Bind("jobID") long jobID,
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

}
