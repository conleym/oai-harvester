package org.unizin.cmp.harvester.agent;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType;
import org.unizin.cmp.oai.harvester.HarvestParams;
import org.unizin.cmp.oai.harvester.Harvester;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

public final class HarvestAgent implements Observer {
    private static final int DEFAULT_QUEUE_CAPACITY = 10000;
    private static final long POLL_TIMEOUT = 100;
    private static final TimeUnit POLL_TIME_UNIT = TimeUnit.MILLISECONDS;
    private static final int BATCH_SIZE = 20;

    private static final Collection<? extends Header> DEFAULT_HEADERS =
            Collections.unmodifiableCollection(Arrays.asList(
                    new BasicHeader("from", "dev@unizin.org")));

    private final HttpClient httpClient;
    private final DynamoDBMapper mapper;
    private final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue;
    private final ExecutorService executorService;
    private final Set<Harvester> harvesters = Collections.synchronizedSet(new HashSet<>());
    private volatile boolean stopped = false;
    private volatile boolean consumerRunning = false;


    public HarvestAgent(final HttpClient httpClient,
            final DynamoDBMapper mapper) {
        this(httpClient, mapper,
                new ArrayBlockingQueue<>(DEFAULT_QUEUE_CAPACITY),
                Executors.newCachedThreadPool());
    }


    public HarvestAgent(final HttpClient httpClient,
            final DynamoDBMapper mapper,
            final BlockingQueue<HarvestedOAIRecord> harvestedRecordQueue,
            final ExecutorService executorService) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.harvestedRecordQueue = harvestedRecordQueue;
        this.executorService = executorService;
    }


    public void addHarvests(final HarvestParams...params)
            throws NoSuchAlgorithmException {
        for (final HarvestParams param : params) {
            final Runnable r = createHarvestRunnable(param);
            executorService.submit(r);
        }
        if (params.length > 0 && !consumerRunning) {
            executorService.submit(createConsumerRunnable());
        }
    }


    private Runnable createHarvestRunnable(final HarvestParams params)
            throws NoSuchAlgorithmException {
        final Harvester harvester = new Harvester.Builder()
                .withHttpClient(httpClient)
                .build();
        harvester.addObserver(this);
        harvesters.add(harvester);
        final OAIResponseHandler handler = new AgentOAIResponseHandler(
                params.getBaseURI(), harvestedRecordQueue);
        return () -> {
            try {
                harvester.start(params, handler);
            } finally {
                harvesters.remove(harvester);
            }
        };
    }


    private HarvestedOAIRecord tryPoll() throws InterruptedException {
        return harvestedRecordQueue.poll(POLL_TIMEOUT, POLL_TIME_UNIT);
    }


    private void stop() {
        stopped = true;
        for (final Harvester h : harvesters) {
            h.stop();
        }
        executorService.shutdownNow();
    }


    private Runnable createConsumerRunnable() {
        return () -> {
            final List<HarvestedOAIRecord> batch = new ArrayList<>(BATCH_SIZE);
            consumerRunning = true;
            try {
                while (! stopped) {
                    try {
                        final HarvestedOAIRecord record = tryPoll();
                        if (record == null) {
                            continue;
                        }
                        batch.add(record);
                        if (batch.size() % BATCH_SIZE == 0) {
                            mapper.batchWrite(batch, Collections.emptyList());
                            batch.clear();
                        }
                    } catch (final InterruptedException e) {
                        Thread.interrupted();
                        stop();
                        throw new HarvesterException(e);
                    }
                    stopped = stopped || harvesters.isEmpty();
                }
                // Write any leftovers from the last batch.
                mapper.batchWrite(batch, Collections.emptyList());
            } finally {
                consumerRunning = false;
                stop();
            }
        };
    }


    @Override
    public void update(final Observable o, final Object arg) {
        final HarvestNotification hn = (HarvestNotification)arg;
        if (hn.getType() == HarvestNotificationType.HARVEST_ENDED) {
            harvesters.remove((Harvester)o);
        }
    }


    public static void main(final String[] args) throws Exception {
        // TODO: these are mine from my own AWS account. They should not be in the final code.
        final AWSCredentials creds = new AWSCredentials() {
            @Override
            public String getAWSSecretKey() {
                return "RUT1OuyWGu7C+qgI9kBi/vmo+JPwxYKYQifNweVI";
            }

            @Override
            public String getAWSAccessKeyId() {
                return "AKIAISCX6PRATPDNKWJA";
            }
        };

        // All three of these things are threadsafe.
        final AmazonDynamoDB dynamo = new AmazonDynamoDBAsyncClient(creds);
        dynamo.setEndpoint("http://localhost:8000");
        final DynamoDBMapper mapper = new DynamoDBMapper(dynamo);
        final HttpClient httpClient = HttpClients.custom()
                .setDefaultHeaders(DEFAULT_HEADERS)
                .build();

        final HarvestAgent agent = new HarvestAgent(httpClient, mapper);
        // TODO get list of harvest params from somewhere and pass them in here.
        final URI uri = new URI("http://kb.osu.edu/oai/request");
        final HarvestParams[] params = { new HarvestParams(uri, OAIVerb.LIST_RECORDS) };
        agent.addHarvests(params);

        // Uncomment to check results.
//       final PaginatedScanList<HarvestedOAIRecord> psl = mapper.scan(HarvestedOAIRecord.class, new DynamoDBScanExpression());
//       for (final HarvestedOAIRecord record : psl) {
//           System.out.println(record);
//       }
    }
}
