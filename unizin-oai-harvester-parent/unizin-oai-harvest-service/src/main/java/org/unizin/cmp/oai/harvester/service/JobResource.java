package org.unizin.cmp.oai.harvester.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.skife.jdbi.v2.DBI;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.harvester.HarvestParams;
import org.unizin.cmp.oai.harvester.job.HarvestJob;
import org.unizin.cmp.oai.harvester.service.JobManager.JobCreationException;

/**
 * Resource responsible for creating jobs and reporting on their statuses.
 */
@Path(JobResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public final class JobResource {

    private static final class Harvests {
        List<Map<String, String>> invalid = new ArrayList<>();
        List<HarvestParams> valid = new ArrayList<>();
    }

    public static final String PATH = "/job/";

    private final DBI dbi;
    private final JobManager jobManager;
    private final ExecutorService executor;


    public JobResource(final DBI dbi,
            final JobManager jobManager,
            final ExecutorService executor) {
        this.dbi = dbi;
        this.jobManager = jobManager;
        this.executor = executor;
    }

    private static OAIVerb verbOf(final String string) {
        switch(string) {
        case "ListRecords": return OAIVerb.LIST_RECORDS;
        case "GetRecord": return OAIVerb.GET_RECORD;
        default: return null;
        }
    }

    private static Harvests params(final List<Map<String, String>> harvests) {
        final Harvests h = new Harvests();
        for (final Map<String, String> harvest : harvests) {
            final OAIVerb verb = verbOf(harvest.remove("verb"));
            if (verb == null) {
                h.invalid.add(harvest);
                continue;
            }
            try {
                final URI baseURI = new URI(harvest.remove("baseURI"));
                final HarvestParams params = new HarvestParams.Builder(baseURI,
                        verb).withMap(harvest).build();
                if (! params.areValid()) {
                    h.invalid.add(harvest);
                } else {
                    h.valid.add(params);
                }
            } catch (final URISyntaxException e) {
                h.invalid.add(harvest);
                continue;
            }
        }
        return h;
    }

    private List<HarvestParams> removeDuplicates(
            final List<HarvestParams> harvests) {
        return new ArrayList<>(new HashSet<>(harvests));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response newJob(final List<Map<String, String>> request)
            throws NoSuchAlgorithmException, URISyntaxException {
        final Harvests h = params(request);
        if (!h.invalid.isEmpty()) {
            final Map<String, Object> m = new HashMap<>(1);
            m.put("invalidHarvests", h.invalid);
            return Response.status(Status.BAD_REQUEST).entity(m).build();
        }
        try {
            final String jobName = jobManager.newJob(executor,
                    removeDuplicates(h.valid));
            return Response.created(new URI(PATH + jobName)).build();
        } catch (final RejectedExecutionException e) {
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (final JobCreationException e) {
            final Map<String, Object> m = new HashMap<>(1);
            m.put("invalidURIs", e.getInvalidBaseURIs());
            return Response.status(Status.BAD_REQUEST).entity(m).build();
        }
    }

    @GET
    @Path("running")
    public Response runningJobs() {
        return Response.ok(jobManager.getRunningStatus()).build();
    }

    private JobStatus readStatusFromDatabase(final long jobID) {
        final JobStatus status = new JobStatus(dbi);
        return status.loadFromDB(jobID) ? status : null;
    }

    @GET
    @Path("{jobID}")
    public Response status(final @PathParam("jobID") long jobID) {
        JobStatus status = jobManager.getStatus(String.valueOf(jobID));
        if (status == null) {
            status = readStatusFromDatabase(jobID);
            if (status == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
        }
        return Response.ok(status).build();
    }

    @PUT
    @Path("{jobID}/stop")
    public Response stop(final @PathParam("jobID") String jobID) {
        final HarvestJob job = jobManager.getJob(jobID);
        if (job == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        job.stop();
        return Response.ok().build();
    }
}
