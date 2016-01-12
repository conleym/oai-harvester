--liquibase formatted sql
--changeset mconley:2 failOnError:true
insert into REPOSITORY(REPOSITORY_BASE_URI) values(
  'https://dspace.library.colostate.edu/oai/request');
insert into REPOSITORY(REPOSITORY_BASE_URI) values(
  'http://kb.osu.edu/oai/request');
insert into REPOSITORY(REPOSITORY_BASE_URI) values(
  'http://scholarworks.iu.edu/dspace-oai/request');
insert into REPOSITORY(REPOSITORY_BASE_URI) values(
  'http://ufdc.ufl.edu/sobekcm_oai.aspx');
insert into REPOSITORY(REPOSITORY_BASE_URI) values(
  'http://digitalcommons.unl.edu/do/oai/');
insert into REPOSITORY(REPOSITORY_BASE_URI) values(
  'http://conservancy.umn.edu/oai/request');
insert into REPOSITORY(REPOSITORY_BASE_URI) values(
  'http://deepblue.lib.umich.edu/dspace-oai/request');
insert into REPOSITORY(REPOSITORY_BASE_URI) values(
  'http://minds.wisconsin.edu/oai/request');
insert into REPOSITORY(REPOSITORY_BASE_URI) values(
  'http://ir.uiowa.edu/do/oai/');
insert into REPOSITORY(REPOSITORY_BASE_URI) values(
  'http://ir.library.oregonstate.edu/oai/request');

