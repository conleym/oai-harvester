package org.unizin.cmp.oai.harvester.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import javax.sql.DataSource;
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

import org.apache.http.client.HttpClient;
import org.jboss.logging.MDC;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.HarvestParams;
import org.unizin.cmp.oai.harvester.Harvester;
import org.unizin.cmp.oai.harvester.job.HarvestJob;
import org.unizin.cmp.oai.harvester.job.JobNotification;
import org.unizin.cmp.oai.harvester.job.JobNotification.JobNotificationType;
import org.unizin.cmp.oai.harvester.service.config.HarvestJobConfiguration;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

@Path(JobResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public final class JobResource {

    private static final class Harvests {
        List<Map<String, String>> invalid = new ArrayList<>();
        List<HarvestParams> valid = new ArrayList<>();
    }

    public static final String PATH = "/job/";


    private final DataSource ds;
    private final HarvestJobConfiguration jobConfig;
    private final HttpClient httpClient;
    private final DynamoDBMapper mapper;
    private final ExecutorService executor;
    private final ConcurrentMap<String, JobStatus> jobStatus =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, HarvestJob> jobs =
            new ConcurrentHashMap<>();


    public JobResource(final DataSource ds,
            final HarvestJobConfiguration jobConfig,
            final HttpClient httpClient,
            final DynamoDBMapper mapper,
            final ExecutorService executor) {
        this.ds = ds;
        this.jobConfig = jobConfig;
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.executor = executor;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response newJob(final List<Map<String, String>> request)
            throws NoSuchAlgorithmException, URISyntaxException {
        final Harvests h = params(request);
        if (!h.invalid.isEmpty()) {
            final Map<String, Object> m = new HashMap<>(1);
            m.put("invalidHarvests", h.invalid);
            return Response.status(Status.BAD_REQUEST)
                    .entity(m).build();
        }
        final String jobName = nextJobName();
        final Observer observeHarvests = (o, arg) -> {
            harvestUpdate(jobName, o, arg);
        };
        final HarvestJob job = jobConfig.buildJob(httpClient, mapper, executor,
                jobName, h.valid, Collections.singletonList(observeHarvests));
        job.addObserver((o, arg) -> jobUpdate(jobName, o, arg));
        jobStatus.put(jobName, new JobStatus());
        jobs.put(jobName, job);
        try {
            executor.submit(() -> {
                MDC.put("jobName", jobName);
                job.start();
            });
        } catch (RejectedExecutionException e) {
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }
        return Response.created(new URI(PATH + jobName)).build();
    }

    private static String nextJobName() {
        return UUID.randomUUID().toString();
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

    private static OAIVerb verbOf(final String string) {
        switch(string) {
        case "ListRecords": return OAIVerb.LIST_RECORDS;
        case "GetRecord": return OAIVerb.GET_RECORD;
        default: return null;
        }
    }

    private void jobUpdate(final String jobName, final Object o,
            final Object arg) {
        if (o instanceof HarvestJob && arg instanceof JobNotification) {
            final JobNotification notification = (JobNotification)arg;
            if (notification.getType() == JobNotificationType.STOPPED) {
                jobStatus.remove(jobName);
            } else {
                final JobStatus status = jobStatus.get(jobName);
                status.jobUpdate(notification);
                jobStatus.put(jobName, status);
            }
        }
    }

    private void harvestUpdate(final String jobName, final Object o,
            final Object arg) {
        if (o instanceof Harvester && arg instanceof HarvestNotification) {
            final JobStatus status = jobStatus.get(jobName);
            status.harvestUpdate((HarvestNotification)arg);
            jobStatus.put(jobName, status);
        }
    }

    @GET
    @Path("running")
    public Response runningJobs() {
        return Response.ok(jobStatus).build();
    }

    @GET
    @Path("{jobID}")
    public Response status(final @PathParam("jobID") String jobID) {
        final Object status = jobStatus.get(jobID);
        if (status == null) {
            // TODO check database.
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(status).build();
    }

    @PUT
    @Path("{jobID}/stop")
    public Response stop(final @PathParam("jobID") String jobID) {
        final HarvestJob job = jobs.get(jobID);
        if (job == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        job.stop();
        return Response.ok().build();
    }
}
