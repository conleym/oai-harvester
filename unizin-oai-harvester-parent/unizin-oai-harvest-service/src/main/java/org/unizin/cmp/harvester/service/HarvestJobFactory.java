package org.unizin.cmp.harvester.service;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Observer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Min;

import org.apache.http.client.HttpClient;
import org.unizin.cmp.harvester.job.HarvestJob;
import org.unizin.cmp.harvester.job.HarvestedOAIRecord;
import org.unizin.cmp.harvester.job.Timeout;
import org.unizin.cmp.oai.harvester.HarvestParams;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Dropwizard configuration class for {@link HarvestJob} instances and for the
 * {@code BlockingQueue}.
 */
public final class HarvestJobFactory {
    private static final Observer[] EMPTY_OBS = new Observer[]{};
    private static final HarvestParams[] EMPTY_PARAMS = new HarvestParams[]{};

    @JsonProperty
    @Min(1)
    private Integer batchSize;

    @JsonProperty
    @Min(1)
    private Integer queueCapacity;

    @JsonProperty
    @Min(1)
    private Long pollTimeoutMillis;

    @JsonProperty
    @Min(1)
    private Long offerTimeoutMillis;


    public BlockingQueue<HarvestedOAIRecord> buildQueue() {
        return new ArrayBlockingQueue<>(queueCapacity);
    }

    public HarvestJob buildJob(final HttpClient httpClient,
            final DynamoDBMapper mapper,
            final BlockingQueue<HarvestedOAIRecord> queue,
            final ExecutorService executor,
            final List<HarvestParams> harvestParams,
            final List<Observer> harvestObservers)
            throws NoSuchAlgorithmException {
        final HarvestJob.Builder builder = new HarvestJob.Builder(mapper)
                .withHttpClient(httpClient)
                .withRecordQueue(queue)
                .withExecutorService(executor)
                .withHarvestObservers(harvestObservers.toArray(EMPTY_OBS))
                .withHarvestParams(harvestParams.toArray(EMPTY_PARAMS));

        if (offerTimeoutMillis != null) {
            builder.withOfferTimeout(new Timeout(offerTimeoutMillis,
                    TimeUnit.MILLISECONDS));
        }
        if (pollTimeoutMillis != null) {
            builder.withPollTimeout(new Timeout(pollTimeoutMillis,
                    TimeUnit.MILLISECONDS));
        }
        if (batchSize != null) {
            builder.withBatchSize(batchSize);
        }
        return builder.build();
    }
}
