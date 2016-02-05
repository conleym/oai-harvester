package org.unizin.cmp.oai.harvester.service;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.apache.http.client.HttpClient;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.MDC;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.HarvestParams;
import org.unizin.cmp.oai.harvester.Harvester;
import org.unizin.cmp.oai.harvester.job.HarvestJob;
import org.unizin.cmp.oai.harvester.job.JobHarvestSpec;
import org.unizin.cmp.oai.harvester.job.JobNotification;
import org.unizin.cmp.oai.harvester.job.JobNotification.JobNotificationType;
import org.unizin.cmp.oai.harvester.service.config.HarvestJobConfiguration;
import org.unizin.cmp.oai.harvester.service.db.DBIUtils;
import org.unizin.cmp.oai.harvester.service.db.H2Functions.JobInfo;

public final class JobManager {
    private final HarvestJobConfiguration jobConfig;
    private final HttpClient httpClient;
    private final DynamoDBClient dynamoClient;
    private final DBI dbi;
    private final ConcurrentMap<String, JobStatus> jobStatus =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, HarvestJob> jobs =
            new ConcurrentHashMap<>();


    public JobManager(final HarvestJobConfiguration jobConfig,
            final HttpClient httpClient, final DynamoDBClient dynamoClient,
            final DBI dbi) {
        this.jobConfig = jobConfig;
        this.httpClient = httpClient;
        this.dynamoClient = dynamoClient;
        this.dbi = dbi;
    }

    private JobInfo addJobToDatabase(final List<HarvestParams> harvests) {
        try (final Handle handle = DBIUtils.handle(dbi)) {
            return handle.createCall("#info = call CREATE_JOB(#paramList)")
                    .bind("paramList", harvests)
                    .registerOutParameter("info", Types.OTHER)
                    .invoke().getObject("info", JobInfo.class);
        }
    }

    private List<JobHarvestSpec> buildSpecs(final String jobName,
            final JobInfo jobInfo, final List<HarvestParams> params) {
        final List<JobHarvestSpec> specs = new ArrayList<>();
        final Iterator<Long> harvestIDs = jobInfo.getHarvestIDs().iterator();
        params.forEach(x -> {
            final Map<String, String> tags = new HashMap<>(1);
            tags.put("harvestName", String.valueOf(harvestIDs.next()));
            specs.add(new JobHarvestSpec(x, tags));
        });
        return specs;
    }

    private void harvestUpdate(final String jobName, final Object o,
            final Object arg) {
        if (o instanceof Harvester && arg instanceof HarvestNotification) {
            final JobStatus status = jobStatus.get(jobName);
            status.harvestUpdate((HarvestNotification)arg);
            jobStatus.put(jobName, status);
        }
    }

    private void jobUpdate(final String jobName, final Object o,
            final Object arg) {
        if (o instanceof HarvestJob && arg instanceof JobNotification) {
            final JobNotification notification = (JobNotification)arg;
            if (notification.getType() == JobNotificationType.STOPPED) {
                jobStatus.get(jobName).jobUpdate(notification);
                jobStatus.remove(jobName);
            } else {
                final JobStatus status = jobStatus.get(jobName);
                status.jobUpdate(notification);
                jobStatus.put(jobName, status);
            }
        }
    }

    /**
     *
     * @param executor
     * @param specs
     * @return the name of the newly-created job.
     *
     * @throws URISyntaxException
     * @throws NoSuchAlgorithmException
     * @throws java.util.concurrent.RejectedExecutionException
     */
    public String newJob(final ExecutorService executor,
            final List<HarvestParams> params)
                    throws URISyntaxException, NoSuchAlgorithmException {
        final JobInfo jobInfo = addJobToDatabase(params);
        final String jobName = String.valueOf(jobInfo.getID());
        final List<JobHarvestSpec> specs = buildSpecs(jobName, jobInfo, params);
        final Observer observeHarvests = (o, arg) -> {
            harvestUpdate(jobName, o, arg);
        };
        final HarvestJob job = jobConfig.job(httpClient,
                dynamoClient.getMapper(), executor,
                jobName, specs, Collections.singletonList(observeHarvests));
        job.addObserver((o, arg) -> jobUpdate(jobName, o, arg));
        jobStatus.put(jobName, new JobStatus(dbi));
        jobs.put(jobName, job);
        executor.submit(() -> {
            MDC.put("jobName", jobName);
            job.start();
        });
        return jobName;
    }

    public HarvestJob getJob(final String jobName) {
        return jobs.get(jobName);
    }

    public SortedMap<String, JobStatus> getRunningStatus() {
        return new TreeMap<>(jobStatus);
    }

    public JobStatus getStatus(final String jobName) {
        return jobStatus.get(jobName);
    }

    public long getMaxQueueSize() {
        final Optional<Long> l = jobStatus.values().stream()
                .map(x -> x.getQueueSize())
                .filter(x -> x != null)
                .max((x,y) -> Long.compare(x, y));
        if (l.isPresent()) {
            return l.get();
        }
        return 0;
    }
}
