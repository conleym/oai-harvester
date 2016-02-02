--liquibase formatted sql
--changeset mconley:0 failOnError:true runOnChange:true splitStatements:false stripComments: true

-- Treatment of aliases is complicated by the fact that they're also tied to
-- the Java code. Refactorings there can make creating the database from scratch
-- via migration impossible (e.g., if the function is defined in a class and
-- that class is later renamed, the first `create alias` for that function will
-- fail, stopping the migration entirely).
--
-- It seems best, therefore, to remove _all_ aliases and restore them on each
-- migration. This migration will run every time a change is detected.

drop alias DROP_ALL_ALIASES if exists;

-- Splitting statements causes errors here -- it must be false.
create alias DROP_ALL_ALIASES as $$
void dropAllAliases(final Connection c) throws SQLException {
    final PreparedStatement ps = c.prepareStatement(
            "select ALIAS_NAME from INFORMATION_SCHEMA.FUNCTION_ALIASES");
    final ResultSet rs = ps.executeQuery();
    while (rs.next()) {
       final PreparedStatement drop = c.prepareStatement("drop alias " +
            rs.getString(1));
       drop.execute();
    }
}$$;

-- Also drops itself, rather conveniently.
call DROP_ALL_ALIASES();


--------------------------------
-- CURRENT ALIASES ONLY BELOW --
--------------------------------

create alias CREATE_JOB for
       "org.unizin.cmp.oai.harvester.service.H2Functions.createJob";

create alias INSERT_OAI_ERRORS for
 "org.unizin.cmp.oai.harvester.service.H2Functions.insertOAIErrors";

create alias OAI_ERRORS for
 "org.unizin.cmp.oai.harvester.service.H2Functions.readOAIErrors";
