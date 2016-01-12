package org.unizin.cmp.oai.harvester.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.OutParameters;
import org.unizin.cmp.oai.harvester.HarvestParams;

/**
 * H2 stored functions.
 * <p>
 * These must be aliased via <tt>CREATE ALIAS</tt>. See <a href=
 * "http://www.h2database.com/html/features.html#user_defined_functions">H2 user
 * defined functions</a> for details.
 */
public final class H2Functions {

    private static long nextValue(final Handle handle, final String sequence) {
        final OutParameters out = handle.createCall(
                ":id = call next value for " + sequence)
                .registerOutParameter("id", Types.BIGINT).invoke();
        return out.getInt("id");
    }

    public static long createJob(final Connection c,
            final List<HarvestParams> params) throws SQLException {
        // Do not close the handle! causes exceptions.
        final Handle h = DBI.open(c);
        final long jobID = nextValue(h, "JOB_ID_SEQ");
        final JobJDBI jdbi = h.attach(JobJDBI.class);
        jdbi.createJob(jobID);
        params.forEach(x -> createHarvest(c, jobID, x));
        return jobID;
    }

    public static long createHarvest(final Connection c,
            final long jobID, final HarvestParams params) {
        // Do not close the handle! causes exceptions.
        final Handle h = DBI.open(c);
        final JobJDBI jdbi = h.attach(JobJDBI.class);
        final long harvestID = nextValue(h, "HARVEST_ID_SEQ");
        final long repositoryID = jdbi.findRepositoryIDByBaseURI(
                params.getBaseURI().toString());
        jdbi.createHarvest(harvestID, jobID, repositoryID,
                params.toString());
        return harvestID;
    }

    public static long createRepository(final Connection c,
            final String baseURI) {
        // Do not close the handle! causes exceptions.
        final Handle h = DBI.open(c);
        final JobJDBI jdbi = h.attach(JobJDBI.class);
        final long repositoryID = nextValue(h, "REPOSITORY_ID_SEQ");
        jdbi.createRepository(repositoryID, baseURI);
        return repositoryID;
    }

    /** No instances allowed. */
    private H2Functions() { }
}
