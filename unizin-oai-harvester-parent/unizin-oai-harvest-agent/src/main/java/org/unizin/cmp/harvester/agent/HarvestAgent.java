package org.unizin.cmp.harvester.agent;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
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
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType;
import org.unizin.cmp.oai.harvester.HarvestParams;
import org.unizin.cmp.oai.harvester.Harvester;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper.FailedBatch;

public final class HarvestAgent implements Observer {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            HarvestAgent.class);

    public static final String DIGEST_ALGORITHM = "MD5";
    public static final int DEFAULT_BATCH_SIZE = 20;
    public static final Timeout DEFAULT_TIMEOUT = new Timeout(100,
            TimeUnit.MILLISECONDS);
    public static final int DEFAULT_QUEUE_CAPACITY = 10 * 1000;

    private static final Collection<? extends Header> DEFAULT_HEADERS =
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

        public HarvestAgent build() {
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
            return new HarvestAgent(httpClient, mapper, harvestedRecordQueue,
                    executorService, offerTimeout, pollTimeout, batchSize);
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
    private final Object runningHarvestersLock = new Object();
    private final Set<Harvester> runningHarvesters = new HashSet<>();
    private volatile boolean stopped;


    public HarvestAgent(final HttpClient httpClient,
            final DynamoDBMapper mapper,
            final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue,
            final ExecutorService executorService,
            final Timeout offerTimeout,
            final Timeout pollTimeout,
            final int batchSize) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.harvestedRecordQueue = harvestedRecordQueue;
        this.executorService = executorService;
        this.offerTimeout = offerTimeout;
        this.pollTimeout = pollTimeout;
        this.batchSize = batchSize;
    }

    public static MessageDigest digest()
            throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(DIGEST_ALGORITHM);
    }

    private void addHarvester(final Harvester harvester) {
        synchronized(runningHarvestersLock) {
            runningHarvesters.add(harvester);
        }
    }

    private void removeHarvester(final Harvester harvester) {
        synchronized(runningHarvestersLock) {
            runningHarvesters.remove(harvester);
        }
    }

    private void removeAllHarvesters() {
        synchronized(runningHarvestersLock) {
            runningHarvesters.forEach(Harvester::stop);
            runningHarvesters.clear();
        }
    }

    public void addHarvests(final HarvestParams...params)
            throws NoSuchAlgorithmException {
        for (final HarvestParams param : params) {
            final Runnable r = createHarvestRunnable(param);
            tasks.add(r);
        }
    }

    private boolean shouldStop() {
        synchronized (runningHarvestersLock) {
            return stopped || runningHarvesters.isEmpty();
        }
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

    public void start() {
        if (tasks.isEmpty()) {
            return;
        }
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
        // Write any leftovers from the last batch.
        LOGGER.info("Writing final batch of {} records to database.",
                batch.size());
        writeBatch(batch);
    }

    private Runnable createHarvestRunnable(final HarvestParams params)
            throws NoSuchAlgorithmException {
        final Harvester harvester = new Harvester.Builder()
                .withHttpClient(httpClient)
                .build();
        harvester.addObserver(this);
        addHarvester(harvester);
        MDC.put("baseURI", params.getBaseURI().toString());
        MDC.put("parameters", params.getParameters().toString());
        final OAIResponseHandler handler = new AgentOAIResponseHandler(
                params.getBaseURI(), harvestedRecordQueue, offerTimeout);
        return () -> {
            try {
                harvester.start(params, handler);
            } catch (final Exception e) {
                LOGGER.error("Error in harvester thread.", e);
            } finally {
                removeHarvester(harvester);
            }
        };
    }

    private HarvestedOAIRecord tryPoll() throws InterruptedException {
        return harvestedRecordQueue.poll(pollTimeout.getTime(),
                pollTimeout.getUnit());
    }

    private void stop() {
        stopped = true;
        LOGGER.info("Shutting down.");
        removeAllHarvesters();
        executorService.shutdownNow();
    }

    @Override
    public void update(final Observable o, final Object arg) {
        boolean ok = true;
        if (! (o instanceof Harvester)) {
            ok = false;
            LOGGER.error(
                    "Observable argument was of type {}. Was expecting {}.",
                    o.getClass().getName(), Harvester.class.getName());
        }
        if (! (arg instanceof HarvestNotification)) {
            ok = false;
            LOGGER.error("Update argument was of type {}. Was expecting {}",
                    arg.getClass().getName(), HarvestNotification.class.getName());
        }
        if (ok) {
            final HarvestNotification hn = (HarvestNotification)arg;
            if (hn.getType() == HarvestNotificationType.HARVEST_ENDED) {
                LOGGER.info("Harvest of {} has ended.", hn.getHarvestParameters());
                removeHarvester((Harvester)o);
            }
        }
    }
}
