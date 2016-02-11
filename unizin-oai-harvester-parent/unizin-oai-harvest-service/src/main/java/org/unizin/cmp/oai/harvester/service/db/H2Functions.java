package org.unizin.cmp.oai.harvester.service.db;

import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * Information about a newly-created job, including information about the
     * harvests which are part of it.
     * <p>
     * Instances are immutable.
     * </p>
     */
    public static final class JobInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private final long id;
        private final List<HarvestInfo> harvests;
        private final List<String> invalidRepositoryBaseURIs;

        public JobInfo(final long id, final List<HarvestInfo> harvests,
                final List<String> invalidRepositoryBaseURIs) {
            this.id = id;
            this.harvests = Collections.unmodifiableList(harvests);
            this.invalidRepositoryBaseURIs = Collections.unmodifiableList(
                    invalidRepositoryBaseURIs);
        }

        public long getID() {
            return id;
        }

        public List<HarvestInfo> getHarvests() {
            return harvests;
        }

        public List<String> getInvalidRepositoryBaseURIs() {
            return invalidRepositoryBaseURIs;
        }
    }

    /**
     * Information about a harvest that is part of a newly-created job.
     * <p>
     * Instances are immutable.
     * </p>
     */
    public static final class HarvestInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final String repositoryInstitution;
        private final String repositoryName;
        private final String repositoryBaseURI;
        private final boolean repositoryExists;

        public HarvestInfo(final Map<String, Object> row,
                final boolean repositoryExists) {
            final Long id = (Long)row.get("HARVEST_ID");
            name = id == null ? null : String.valueOf(id);
            repositoryInstitution = (String)row.get("REPOSITORY_INSTITUTION");
            repositoryName = (String)row.get("REPOSITORY_NAME");
            repositoryBaseURI = (String)row.get("REPOSITORY_BASE_URI");
            this.repositoryExists = repositoryExists;
        }

        public String getRepositoryInstitution() {
            return repositoryInstitution;
        }

        public String getRepositoryName() {
            return repositoryName;
        }

        public String getRepositoryBaseURI() {
            return repositoryBaseURI;
        }

        public String getName() {
            return name;
        }

        public boolean repositoryExists() {
            return repositoryExists;
        }
    }


    private static H2FunctionsJDBI h2DBI(final Handle h) {
        return h.attach(H2FunctionsJDBI.class);
    }

    private static JobJDBI jobDBI(final Handle h) {
        return h.attach(JobJDBI.class);
    }

    /**
     * Create a new JOB row and a corresponding HARVEST row for each set of
     * harvest parameters supplied.
     *
     * @param c
     *            the database connection (supplied by H2).
     * @param params
     *            the list of parameters for harvests in this job.
     * @return information about the job.
     */
    public static JobInfo createJob(final Connection c,
            final List<HarvestParams> params) {
        // Do not close the handle! causes exceptions.
        final Handle h = DBI.open(c);
        final JobJDBI jdbi = jobDBI(h);
        final long jobID = jdbi.createJob();
        final List<HarvestInfo> harvests = new ArrayList<>();
        params.forEach(x -> harvests.add(createHarvest(c, jobID, x)));
        final List<String> invalidURIs = harvests.stream()
                .filter(x -> !x.repositoryExists())
                .map(HarvestInfo::getRepositoryBaseURI)
                .collect(Collectors.toList());
        if (!invalidURIs.isEmpty()) {
            h.rollback();
        }
        return new JobInfo(jobID, harvests, invalidURIs);
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
     * @return information about the harvest.
     */
    private static HarvestInfo createHarvest(final Connection c,
            final long jobID, final HarvestParams params) {
        // Do not close the handle! causes exceptions.
        final Handle h = DBI.open(c);
        final H2FunctionsJDBI h2dbi = h2DBI(h);
        final String baseURI = params.getBaseURI().toString();
        Map<String, Object> repo = h2dbi.findRepositoryIDByBaseURI(
                baseURI);
        if (repo == null) {
            repo = new HashMap<>();
            // So it's available even if the repository doesn't exist.
            repo.put("REPOSITORY_BASE_URI", baseURI);
            return new HarvestInfo(repo, false);
        }
        final long repositoryID = (long)repo.get("REPOSITORY_ID");
        final long id =  h2dbi.createHarvest(jobID, repositoryID,
                params.getParameters().toString(), params.getVerb());
        repo.put("HARVEST_ID", id);
        return new HarvestInfo(repo, true);
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
        final H2FunctionsJDBI h2dbi = h2DBI(h);
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
        final H2FunctionsJDBI h2dbi = h2DBI(h);
        final List<OAIError> errors = new ArrayList<>();
        h2dbi.readOAIErrors(harvestID).forEach(m -> {
            errors.add(new OAIError(
                    (String)m.get("HARVEST_PROTOCOL_ERROR_CODE"),
                    (String)m.get("HARVEST_PROTOCOL_ERROR_MESSAGE")));
        });
        return errors;
    }

    /**
     * Updates the REPOSITORY table.
     * <p>
     * This function uses
     * <a href="http://jdbi.org/sql_object_api_batching/">JDBI object
     * batching</a>. Each column to be updated is represented by a separate
     * list, with the elements of each list at index <i>i</i> corresponding to
     * the elements of the <i>i<sup>th</sup></i> row.
     * </p>
     * <p>
     * The given rows are inserted into a temporary table, which is then
     * compared against the existing REPOSITORY entries. Those present only in
     * REPOSITORY are disabled, those present only in the temporary table are
     * added (enabled), and those present in both are updated.
     * </p>
     *
     * @param c
     *            the database connection (supplied by H2).
     * @param names
     *            the name of each repository, in order.
     * @param baseURIs
     *            the base URI of each repository, in order.
     * @param institutions
     *            the institution of each repository, in order.
     */
    public static void updateRepositories(final Connection c,
            final List<String> names, final List<String> baseURIs,
            final List<String> institutions) {
        final Handle h = DBI.open(c);
        final H2FunctionsJDBI h2dbi = h2DBI(h);
        h2dbi.createReposTempTable();
        // It's somewhat troubling that this is required to make the test work.
        // local temporary tables shouldn't survive their connections, right?
        h2dbi.clearTempTable();
        h2dbi.addReposToTempTable(names, baseURIs, institutions);
        h2dbi.disableReposNotInNuxeo();
        h2dbi.mergeNuxeoUpdates();
    }

    /** No instances allowed. */
    private H2Functions() { }
}
