package org.unizin.cmp.harvester.service.config;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Observer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Min;

import org.apache.http.client.HttpClient;
import org.hibernate.validator.constraints.NotEmpty;
import org.unizin.cmp.harvester.job.HarvestJob;
import org.unizin.cmp.harvester.job.HarvestedOAIRecord;
import org.unizin.cmp.harvester.job.Timeout;
import org.unizin.cmp.oai.harvester.HarvestParams;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.lifecycle.setup.ExecutorServiceBuilder;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;


/**
 * Dropwizard configuration class for {@link HarvestJob} instances and for the
 * {@code BlockingQueue}.
 */
public final class HarvestJobConfiguration {
    private static final Observer[] EMPTY_OBS = new Observer[]{};
    private static final HarvestParams[] EMPTY_PARAMS = new HarvestParams[]{};

    @JsonProperty
    @Min(1)
    private Integer batchSize;

    @JsonProperty
    @Min(1)
    private Integer queueCapacity;

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
    @NotEmpty
    private String nameFormat = "something-%s";

    public ExecutorService buildExecutorService(final Environment env) {
        final ExecutorServiceBuilder b = env.lifecycle()
                .executorService(nameFormat);
        if (minThreads != null) {
            b.minThreads(minThreads);
        }
        if (maxThreads != null) {
            b.maxThreads(maxThreads);
        }
        return b.build();
    }

    public HarvestJob buildJob(final HttpClient httpClient,
            final DynamoDBMapper mapper,
            final ExecutorService executor,
            final List<HarvestParams> harvestParams,
            final List<Observer> harvestObservers)
            throws NoSuchAlgorithmException {
        final HarvestJob.Builder builder = new HarvestJob.Builder(mapper)
                .withHttpClient(httpClient)
                .withExecutorService(executor)
                .withHarvestObservers(harvestObservers.toArray(EMPTY_OBS))
                .withHarvestParams(harvestParams.toArray(EMPTY_PARAMS));

        if (queueCapacity != null) {
            builder.withRecordQueue(new ArrayBlockingQueue<HarvestedOAIRecord>(
                    queueCapacity));
        }
        if (offerTimeout != null) {
            final long millis = offerTimeout.toMilliseconds();
            builder.withOfferTimeout(new Timeout(millis,
                    TimeUnit.MILLISECONDS));
        }
        if (pollTimeout != null) {
            final long millis = pollTimeout.toMilliseconds();
            builder.withPollTimeout(new Timeout(millis,
                    TimeUnit.MILLISECONDS));
        }
        if (batchSize != null) {
            builder.withBatchSize(batchSize);
        }
        return builder.build();
    }
}
