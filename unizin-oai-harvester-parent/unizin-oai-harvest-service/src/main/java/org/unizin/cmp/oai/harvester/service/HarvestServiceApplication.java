package org.unizin.cmp.oai.harvester.service;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.http.client.HttpClient;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.harvester.job.HarvestedOAIRecord;
import org.unizin.cmp.oai.harvester.service.config.DynamoDBConfiguration;
import org.unizin.cmp.oai.harvester.service.config.HarvestHttpClientBuilder;
import org.unizin.cmp.oai.harvester.service.config.HarvestServiceConfiguration;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.StreamSpecification;
import com.amazonaws.services.dynamodbv2.model.StreamViewType;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.java8.jdbi.DBIFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * The dropwizard application class for the harvest service.
 */
public final class HarvestServiceApplication
extends Application<HarvestServiceConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            HarvestServiceApplication.class);
    private static final String HTTP_CLIENT_NAME =
            "HarvestService HTTP Client";

    private AmazonDynamoDB dynamoDBClient;
    private DynamoDBMapper dynamoDBMapper;
    private ObjectMapper objectMapper;

    @Override
    public void initialize(
            final Bootstrap<HarvestServiceConfiguration> bootstrap) {
        super.initialize(bootstrap);
        bootstrap.addBundle(new Java8Bundle());
        bootstrap.addBundle(
                new MigrationsBundle<HarvestServiceConfiguration>() {
                    @Override
                    public DataSourceFactory getDataSourceFactory(
                            final HarvestServiceConfiguration configuration) {
                        return configuration.getDataSourceFactory();
                    }
                });
        objectMapper = bootstrap.getObjectMapper();
    }

    private void createMapper(
            final DynamoDBConfiguration configuration) {
        dynamoDBClient = configuration.build();
        dynamoDBMapper = configuration.getRecordMapperConfiguration().build(
                dynamoDBClient);
    }

    private void createDynamoDBTable(final DynamoDBConfiguration config) {
        final ProvisionedThroughput throughput = config.buildThroughput();
        final StreamSpecification streamSpec = new StreamSpecification()
                .withStreamEnabled(true)
                .withStreamViewType(StreamViewType.NEW_AND_OLD_IMAGES);
        final CreateTableRequest req = dynamoDBMapper
                .generateCreateTableRequest(HarvestedOAIRecord.class)
                .withProvisionedThroughput(throughput)
                .withStreamSpecification(streamSpec);
        try {
            dynamoDBClient.createTable(req);
        } catch (final ResourceInUseException e) {
            LOGGER.warn("Exception creating DynamoDB table. It probably "
                    + "already exists.", e);
        }
    }

    private void startH2Servers(final HarvestServiceConfiguration conf,
            final Environment env) throws Exception {
        final List<ManagedH2Server> managed = conf
                .getH2ServerConfiguration()
                .build();
        for (final ManagedH2Server server : managed) {
            env.lifecycle().manage(server);
        }
    }

    @Override
    public void run(final HarvestServiceConfiguration conf,
            final Environment env) throws Exception {
        final DBI jdbi = new DBIFactory().build(env,
                conf.getDataSourceFactory(), "database");
        final HttpClient httpClient = new HarvestHttpClientBuilder(env)
                .using(conf.getHttpClientConfiguration())
                .build(HTTP_CLIENT_NAME);
        final ExecutorService executor = conf.getJobConfiguration()
                .buildExecutorService(env);
        final DynamoDBConfiguration dynamo = conf.getDynamoDBConfiguration();
        createMapper(dynamo);
        createDynamoDBTable(dynamo);
        startH2Servers(conf, env);
        final JobResource jr = new JobResource(jdbi, objectMapper,
                conf.getJobConfiguration(), httpClient, dynamoDBMapper,
                executor);
        env.jersey().register(jr);
    }

    public static void main(final String[] args) throws Exception {
        new HarvestServiceApplication().run(args);
    }
}
