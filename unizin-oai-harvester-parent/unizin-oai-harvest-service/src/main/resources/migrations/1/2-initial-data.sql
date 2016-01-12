--liquibase formatted sql
--changeset mconley:2 failOnError:true
create alias CREATE_HARVEST for
       "org.unizin.cmp.oai.harvester.service.H2Functions.createHarvest";
--rollback drop alias CREATE_HARVEST;
create alias CREATE_JOB for
       "org.unizin.cmp.oai.harvester.service.H2Functions.createJob";
--rollback drop alias CREATE_JOB;
create alias CREATE_REPOSITORY for
       "org.unizin.cmp.oai.harvester.service.H2Functions.createRepository";
--rollback drop alias CREATE_REPOSITORY;

call CREATE_REPOSITORY('https://dspace.library.colostate.edu/oai/request');
call CREATE_REPOSITORY('http://kb.osu.edu/oai/request');
call CREATE_REPOSITORY('http://scholarworks.iu.edu/dspace-oai/request');
call CREATE_REPOSITORY('http://ufdc.ufl.edu/sobekcm_oai.aspx');
call CREATE_REPOSITORY('http://digitalcommons.unl.edu/do/oai/');
call CREATE_REPOSITORY('http://conservancy.umn.edu/oai/request');
call CREATE_REPOSITORY('http://deepblue.lib.umich.edu/dspace-oai/request');
call CREATE_REPOSITORY('http://minds.wisconsin.edu/oai/request');
call CREATE_REPOSITORY('http://ir.uiowa.edu/do/oai/');
call CREATE_REPOSITORY('http://ir.library.oregonstate.edu/oai/request');
