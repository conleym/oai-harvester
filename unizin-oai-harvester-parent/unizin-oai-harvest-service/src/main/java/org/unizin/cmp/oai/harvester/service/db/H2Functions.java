package org.unizin.cmp.oai.harvester.service.db;

import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.unizin.cmp.oai.OAIError;
import org.unizin.cmp.oai.harvester.HarvestParams;

/**
 * H2 stored functions.
 * <p>
 * These must be aliased via <tt>CREATE ALIAS</tt>. See <a href=
 * "http://www.h2database.com/html/features.html#user_defined_functions">H2 user
 * defined functions</a> for details.
 * </p>
 */
public final class H2Functions {
    /**
     * Information about a newly-created job, including the identifiers of all
     * harvests which are part of it.
     * <p>
     * Instances are immutable.
     * </p>
     */
    public static final class JobInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private final long id;
        private final List<Long> harvestIDs;

        public JobInfo(final long id, final List<Long> harvestIDs) {
            this.id = id;
            this.harvestIDs = Collections.unmodifiableList(harvestIDs);
        }

        public long getID() {
            return id;
        }

        public List<Long> getHarvestIDs() {
            return harvestIDs;
        }
    }

    /**
     * Create a new JOB row and a corresponding HARVEST row for each set of
     * harvest parameters supplied.
     *
     * @param c
     *            the database connection (supplied by H2).
     * @param params
     *            the list of parameters for harvests in this job.
     * @return the id of the new JOB row and of each of the HARVEST rows.
     */
    public static JobInfo createJob(final Connection c,
            final List<HarvestParams> params) {
        // Do not close the handle! causes exceptions.
        final Handle h = DBI.open(c);
        final JobJDBI jdbi = h.attach(JobJDBI.class);
        final long jobID = jdbi.createJob();
        final List<Long> harvestIDs = new ArrayList<>();
        params.forEach(x -> harvestIDs.add(createHarvest(c, jobID, x)));
        return new JobInfo(jobID, harvestIDs);
    }

    /**
     * Create a new harvest.
     *
     * @param c
     *            the database connection (supplied by H2).
     * @param jobID
     *            the id of the job of which this harvest is a part.
     * @param params
     *            the parameters of the harvest.
     * @return the new harvest's database identifier.
     */
    private static long createHarvest(final Connection c,
            final long jobID, final HarvestParams params) {
        // Do not close the handle! causes exceptions.
        final Handle h = DBI.open(c);
        final JobJDBI jdbi = h.attach(JobJDBI.class);
        final long repositoryID = jdbi.findRepositoryIDByBaseURI(
                params.getBaseURI().toString());
        final H2FunctionsJDBI h2dbi = h.attach(H2FunctionsJDBI.class);
        return h2dbi.createHarvest(jobID, repositoryID,
                params.getParameters().toString(), params.getVerb());
    }

    /**
     * Record OAI protocol errors for a harvest.
     *
     * @param c
     *            the database connection (supplied by H2).
     * @param harvestID
     *            the id of the harvest that had the errors.
     * @param errors
     *            the OAI errors to record.
     */
    public static void insertOAIErrors(final Connection c, final long harvestID,
            final List<OAIError> errors) {
        // Do not close the handle! causes exceptions.
        final Handle h = DBI.open(c);
        final H2FunctionsJDBI h2dbi = h.attach(H2FunctionsJDBI.class);
        errors.forEach(e -> h2dbi.insertHarvestProtocolError(harvestID,
                e.getMessage(), e.getErrorCodeString()));
    }

    /**
     * Get the list of OAI protocol errors for a given harvest.
     *
     * @param c
     *            the database connection (supplied by H2).
     *
     * @param harvestID
     *            the harvest id.
     * @return a list of the OAI protocol errors for this harvest.
     */
    public static List<OAIError> readOAIErrors(final Connection c,
            final long harvestID) {
       // Do not close the handle! causes exceptions.
       final Handle h = DBI.open(c);
       final H2FunctionsJDBI h2dbi = h.attach(H2FunctionsJDBI.class);
       final List<OAIError> errors = new ArrayList<>();
       h2dbi.readOAIErrors(harvestID).forEach(m -> {
           errors.add(new OAIError(
                   (String)m.get("HARVEST_PROTOCOL_ERROR_CODE"),
                   (String)m.get("HARVEST_PROTOCOL_ERROR_MESSAGE")));
       });
       return errors;
    }

    /** No instances allowed. */
    private H2Functions() { }
}
