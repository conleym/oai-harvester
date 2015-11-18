package org.unizin.cmp.harvester.agent;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Observer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.unizin.cmp.oai.harvester.HarvestParams;
import org.unizin.cmp.oai.harvester.Harvester;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;

/**
 * A combination of a single consumer and one or more producer threads.
 * <p>
 * The producers create {@link HarvestedOAIRecord} instances and place them on a
 * {@link BlockingQueue}. The consumer reads objects from this queue and writes
 * them in batches to DynamoDB.
 * </p>
 * <p>
 * Instances are safe for use in multiple threads.
 * </p>
 */
public final class HarvestAgent {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            HarvestAgent.class);

    public static final String DIGEST_ALGORITHM = "MD5";
    public static final int DEFAULT_BATCH_SIZE = 20;
    public static final Timeout DEFAULT_TIMEOUT = new Timeout(100,
            TimeUnit.MILLISECONDS);
    public static final int DEFAULT_QUEUE_CAPACITY = 10 * 1000;

    public static final Collection<? extends Header> DEFAULT_HEADERS =
            Collections.unmodifiableCollection(Arrays.asList(
                    new BasicHeader("from", "dev@unizin.org")));


    public static final class Builder {
        private final DynamoDBMapper mapper;

        private int batchSize = DEFAULT_BATCH_SIZE;
        private Timeout offerTimeout;
        private Timeout pollTimeout;
        private HttpClient httpClient;
        private BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue;
        private ExecutorService executorService;
        private List<HarvestParams> harvestParams;
        private List<Observer> harvestObservers;


        public Builder(final DynamoDBMapper mapper) {
            Objects.requireNonNull(mapper, "mapper");
            this.mapper = mapper;
        }

        public Builder withHttpClient(final HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder withRecordQueue(
                final BlockingQueue<HarvestedOAIRecord> queue) {
            this.harvestedRecordQueue = queue;
            return this;
        }

        public Builder withExecutorService(
                final ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public Builder withOfferTimeout(final Timeout timeout) {
            this.offerTimeout = timeout;
            return this;
        }

        public Builder withPollTimeout(final Timeout timeout) {
            this.pollTimeout = timeout;
            return this;
        }

        public Builder withBatchSize(final int batchSize) {
            if (batchSize <= 0) {
                throw new IllegalArgumentException(
                        "batchSize must be positive.");
            }
            this.batchSize = batchSize;
            return this;
        }

        public Builder withHarvestObservers(final Observer...observers) {
            harvestObservers = Arrays.asList(observers);
            return this;
        }

        public Builder withHarvestParams(final HarvestParams...params) {
            harvestParams = Arrays.asList(params);
            return this;
        }

        public HarvestAgent build() throws NoSuchAlgorithmException {
            if (httpClient == null) {
                httpClient = HttpClients.custom()
                        .setDefaultHeaders(DEFAULT_HEADERS)
                        .build();
            }
            if (harvestedRecordQueue == null) {
                harvestedRecordQueue = new ArrayBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
            }
            if (executorService == null) {
                executorService = Executors.newCachedThreadPool();
            }
            if (offerTimeout == null) {
                offerTimeout = DEFAULT_TIMEOUT;
            }
            if (pollTimeout == null) {
                pollTimeout = DEFAULT_TIMEOUT;
            }
            if (harvestParams == null) {
                harvestParams = Collections.emptyList();
            }
            if (harvestObservers == null) {
                harvestObservers = Collections.emptyList();
            }
            return new HarvestAgent(httpClient, mapper, harvestedRecordQueue,
                    executorService, offerTimeout, pollTimeout, batchSize,
                    harvestParams, harvestObservers);
        }
    }


    private final HttpClient httpClient;
    private final DynamoDBMapper mapper;
    private final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue;
    private final List<Runnable> tasks = new ArrayList<>();
    private final ExecutorService executorService;
    private final Timeout offerTimeout;
    private final Timeout pollTimeout;
    private final int batchSize;
    private final RunningHarvesters runningHarvesters =
            new RunningHarvesters();
    private volatile boolean running;


    public HarvestAgent(final HttpClient httpClient,
            final DynamoDBMapper mapper,
            final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue,
            final ExecutorService executorService,
            final Timeout offerTimeout,
            final Timeout pollTimeout,
            final int batchSize,
            final List<HarvestParams> harvestParams,
            final List<Observer> harvestObservers) throws NoSuchAlgorithmException {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.harvestedRecordQueue = harvestedRecordQueue;
        this.executorService = executorService;
        this.offerTimeout = offerTimeout;
        this.pollTimeout = pollTimeout;
        this.batchSize = batchSize;

        for (final HarvestParams hp: harvestParams) {
            final Runnable r = createHarvestRunnable(hp, harvestObservers);
            tasks.add(r);
        }
    }

    public static MessageDigest digest()
            throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(DIGEST_ALGORITHM);
    }

    private Runnable createHarvestRunnable(final HarvestParams params,
            final Iterable<Observer> observers) throws NoSuchAlgorithmException {
        final Harvester harvester = new Harvester.Builder()
                .withHttpClient(httpClient)
                .build();
        observers.forEach(harvester::addObserver);
        final OAIResponseHandler handler = new AgentOAIResponseHandler(
                params.getBaseURI(), harvestedRecordQueue, offerTimeout);
        final Runnable harvest = () -> {
            MDC.put("baseURI", params.getBaseURI().toString());
            MDC.put("parameters", params.getParameters().toString());
            try {
                harvester.start(params, handler);
            } catch (final Exception e) {
                LOGGER.error("Error in harvester thread.", e);
            }
        };
        return runningHarvesters.wrappedRunnable(harvester, harvest);
    }

    public void start() {
        if (running || tasks.isEmpty()) {
            return;
        }
        running = true;
        tasks.forEach(executorService::submit);
        tasks.clear();
        final List<HarvestedOAIRecord> batch = new ArrayList<>(batchSize);
        while (!shouldStop()) {
            try {
                final HarvestedOAIRecord record = tryPoll();
                if (record == null) {
                    continue;
                }
                batch.add(record);
                if (batch.size() % batchSize == 0) {
                    LOGGER.info("Writing {} records to database.",
                            batch.size());
                    writeBatch(batch);
                    batch.clear();
                }
            } catch (final InterruptedException e) {
                LOGGER.warn("Consumer interrupted. Stopping.", e);
                Thread.interrupted();
                stop();
            }
        }
        if (!batch.isEmpty()) {
            // Write any leftovers from the last batch.
            LOGGER.info("Writing final batch of {} records to database.",
                    batch.size());
            writeBatch(batch);
        }
    }

    private boolean shouldStop() {
        return !running || runningHarvesters.isEmpty();
    }

    private HarvestedOAIRecord tryPoll() throws InterruptedException {
        return harvestedRecordQueue.poll(pollTimeout.getTime(),
                pollTimeout.getUnit());
    }

    public void stop() {
        LOGGER.info("Shutting down.");
        runningHarvesters.cancelAll();
        running = false;
    }

    private void writeBatch(final List<HarvestedOAIRecord> batch) {
        try {
            final List<FailedBatch> failed = mapper.batchWrite(batch,
                    Collections.emptyList());
            if (!failed.isEmpty() && LOGGER.isErrorEnabled()) {
                final StringBuilder sb = new StringBuilder("Batch failed: " +
                        batch + "\t[");
                failed.forEach((fb) -> {
                    sb.append(fb.getClass().getName())
                    .append("[unprocessedItems=")
                    .append(fb.getUnprocessedItems())
                    .append(", exception=")
                    .append(fb.getException())
                    .append("]");
                });
                sb.append("]");
                LOGGER.error(sb.toString());
            }
        } catch (final AmazonClientException e) {
            if (LOGGER.isErrorEnabled()) {
                final String msg = "Error writing batch: " + batch;
                LOGGER.error(msg, e);
            }
        }
    }
}
