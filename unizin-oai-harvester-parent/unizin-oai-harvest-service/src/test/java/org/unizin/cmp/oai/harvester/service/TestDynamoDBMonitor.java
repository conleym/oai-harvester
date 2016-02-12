package org.unizin.cmp.oai.harvester.service;

import java.time.Duration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.unizin.cmp.oai.harvester.job.DynamoDBTestClient;
import org.unizin.cmp.oai.harvester.service.config.HarvestServiceConfiguration;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;

import io.dropwizard.testing.junit.DropwizardAppRule;

public final class TestDynamoDBMonitor {
    /**
     * Value to which write capacity will be set. Must be different from the
     * configured initial write capacity for the table.
     */
    private static final long CAPACITY = 4000;

    @ClassRule
    public static final DropwizardAppRule<HarvestServiceConfiguration> app =
        ServiceTests.newAppRule();

    private static final LazyApp lazy = new LazyApp(app);

    private final DynamoDBTestClient dynamoDBTestClient =
            new DynamoDBTestClient(this.getClass().getSimpleName());

    @Before
    public void before() {
        try {
            dynamoDBTestClient.dropTable();
        } catch (final ResourceNotFoundException e) {
            /*
             * Don't care. Just want to guard against preexisting table screwing
             * things up.
             */
        }
        dynamoDBTestClient.createTable();
        Assert.assertEquals(0, dynamoDBTestClient.countItems());
    }

    private long writeCapacity(final DynamoDBClient client) {
        return client.getThroughputDescription().getWriteCapacityUnits();
    }

    private void assertCapacity(final DynamoDBClient client,
            final long expectedWriteCapacity) {
        final long actual = writeCapacity(client);
        Assert.assertEquals(String.format("Table write capacity should be %d",
                expectedWriteCapacity), expectedWriteCapacity, actual);
    }

    @Test
    public void testSetCapacity() {
        final DynamoDBClient client = lazy.dynamoClient();
        final String msg = "Initial capacity should not be the same as the " +
                "capacity to set.";
        Assert.assertFalse(msg, CAPACITY == writeCapacity(client));
        client.setThroughput(1, CAPACITY);
        assertCapacity(client, CAPACITY);
    }

    private int runRepeatedly(final DynamoDBMonitor monitor,
            final DynamoDBClient client, final long initialCapacity,
            final long expectedCapacity, final int maxRuns) {
        int numRuns = 0;
        client.setThroughput(1, CAPACITY);
        assertCapacity(client, initialCapacity);
        long currentCapacity = initialCapacity;
        while (numRuns < maxRuns && writeCapacity(client) != expectedCapacity) {
            numRuns++;
            currentCapacity /= 2;
            monitor.run();
            assertCapacity(client, currentCapacity);
        }
        assertCapacity(client, expectedCapacity);
        return numRuns;
    }

    private JobManager jobManager(final DynamoDBClient client) {
        return new JobManager(
                app.getConfiguration().getJobConfiguration(),
                // not making any jobs => don't need httpclient.
                null, client, lazy.dbi(), x -> {});
    }

    private DynamoDBMonitor monitor(final DynamoDBClient client,
            final Duration cooldown) {
        return new DynamoDBMonitor(client, jobManager(client), 500, cooldown,
                50, cooldown, 1, 1200);
    }

    @Test
    public void testDecreasesToMinimum() {
        final DynamoDBClient client = lazy.dynamoClient();
        final DynamoDBMonitor monitor = monitor(client, Duration.ofSeconds(0));
        final int numRuns = runRepeatedly(monitor, client, CAPACITY, 1, 12);
        Assert.assertEquals(11, numRuns);
    }

    @Test
    public void testObeysCooldown() throws Exception {
        final DynamoDBClient client = lazy.dynamoClient();
        final DynamoDBMonitor monitor = monitor(client, Duration.ofSeconds(10));
        client.setThroughput(1, CAPACITY);
        monitor.run();
        assertCapacity(client, CAPACITY);
        Thread.sleep(1000 * 15); // more than 10s.
        monitor.run();
        assertCapacity(client, CAPACITY / 2);
    }
}
