package org.unizin.cmp.oai.harvester.service.config;

import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Observer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;

import javax.validation.constraints.Min;

import org.apache.http.client.HttpClient;
import org.hibernate.validator.constraints.NotEmpty;
import org.unizin.cmp.oai.harvester.job.HarvestJob;
import org.unizin.cmp.oai.harvester.job.HarvestedOAIRecord;
import org.unizin.cmp.oai.harvester.job.JobHarvestSpec;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.lifecycle.setup.ExecutorServiceBuilder;
import io.dropwizard.setup.Environment;


/**
 * Dropwizard configuration class for {@link HarvestJob} instances and for the
 * {@code BlockingQueue}.
 */
public final class HarvestJobConfiguration {
    private static final Observer[] EMPTY_OBS = new Observer[]{};
    private static final JobHarvestSpec[] EMPTY_SPECS = new JobHarvestSpec[]{};

    @JsonProperty
    @Min(1)
    private Integer batchSize;

    @JsonProperty
    @Min(1)
    private Integer recordQueueCapacity;

    @JsonProperty
    private Duration pollTimeout;

    @JsonProperty
    private Duration offerTimeout;

    @JsonProperty
    @Min(0)
    private Integer minThreads;

    @JsonProperty
    @Min(1)
    private Integer maxThreads;

    @JsonProperty
    @Min(1)
    private Integer workQueueCapacity;

    @JsonProperty
    @NotEmpty
    private String nameFormat = "harvest-job-%s";

    public ExecutorService buildExecutorService(final Environment env) {
        final ExecutorServiceBuilder b = env.lifecycle()
                .executorService(nameFormat);
        if (minThreads != null) {
            b.minThreads(minThreads);
        }
        if (maxThreads != null) {
            b.maxThreads(maxThreads);
        }
        if (workQueueCapacity != null) {
            b.workQueue(new ArrayBlockingQueue<>(workQueueCapacity));
        }
        return b.build();
    }

    public HarvestJob buildJob(final HttpClient httpClient,
            final DynamoDBMapper mapper,
            final ExecutorService executor,
            final String name,
            final List<JobHarvestSpec> specs,
            final List<Observer> harvestObservers)
            throws NoSuchAlgorithmException, URISyntaxException {
        final HarvestJob.Builder builder = new HarvestJob.Builder(mapper)
                .withHttpClient(httpClient)
                .withExecutorService(executor)
                .withHarvestObservers(harvestObservers.toArray(EMPTY_OBS))
                .withSpecs(specs.toArray(EMPTY_SPECS));

        if (recordQueueCapacity != null) {
            builder.withRecordQueue(new ArrayBlockingQueue<HarvestedOAIRecord>(
                    recordQueueCapacity));
        }
        if (offerTimeout != null) {
            builder.withOfferTimeout(offerTimeout);
        }
        if (pollTimeout != null) {
            builder.withPollTimeout(offerTimeout);
        }
        if (batchSize != null) {
            builder.withBatchSize(batchSize);
        }
        return builder.withName(name).build();
    }
}
