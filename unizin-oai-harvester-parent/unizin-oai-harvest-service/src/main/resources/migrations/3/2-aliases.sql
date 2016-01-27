--liquibase formatted sql
--changeset mconley:6 failOnError:true
drop alias CREATE_HARVEST;
-- rollback create alias CREATE_HARVEST for "org.unizin.cmp.oai.harvester.service.H2Functions.createHarvest";
create alias INSERT_OAI_ERRORS for
 "org.unizin.cmp.oai.harvester.service.H2Functions.insertOAIErrors";

 create alias OAI_ERRORS for
 "org.unizin.cmp.oai.harvester.service.H2Functions.readOAIErrors";
