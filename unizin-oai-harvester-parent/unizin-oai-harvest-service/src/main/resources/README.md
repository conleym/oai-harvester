# Database Migrations

## Files and Directories

The following are used for database migrations:

1. *migrations.xml* is the file dropwizard-migrations consults to perform
 migrations. It simply includes everything in the *migrations* directory.
2. *migrations/* contains all the changesets. Changesets are executed in
 lexicographic order by file (including directory).
3. *sql/* contains SQL that is run as part of a migration but is not itself a
 migration (if we put it in *migrations/*, liquibase would run it twice). These
 can be referenced by a changesets via `sqlFile`.

SQL-formatted changesets should be avoided -- they have limited
support in liquibase. In particular, most preconditions are
unsupported when using the SQL format. When SQL is convenient or
necessary, it should be placed in *sql/* and included in the changeset
via `sqlFile` or included directly in the changeset via `sql`.


 ## Running Migrations
 See [dropwizard-migrations](https://dropwizard.github.io/dropwizard/0.9.2/docs/manual/migrations.html).
