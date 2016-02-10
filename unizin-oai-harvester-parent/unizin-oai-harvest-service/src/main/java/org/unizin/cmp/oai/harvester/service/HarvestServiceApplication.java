package org.unizin.cmp.oai.harvester.service;

import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.apache.http.client.HttpClient;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.service.config.DynamoDBConfiguration;
import org.unizin.cmp.oai.harvester.service.config.HarvestHttpClientBuilder;
import org.unizin.cmp.oai.harvester.service.config.HarvestJobConfiguration;
import org.unizin.cmp.oai.harvester.service.config.HarvestServiceConfiguration;
import org.unizin.cmp.oai.harvester.service.config.JIRAClientConfiguration;
import org.unizin.cmp.oai.harvester.service.config.NuxeoClientConfiguration;
import org.unizin.cmp.oai.harvester.service.db.ManagedH2Server;

import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

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
    private static final String DBI_NAME = "HarvestService Database Connection";

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
        final ObjectMapper objectMapper = bootstrap.getObjectMapper();
        final SimpleModule m = new SimpleModule();
        m.addSerializer(new JsonSerializer<Blob>() {
            @Override
            public void serialize(final Blob value, final JsonGenerator gen,
                    final SerializerProvider serializers)
                    throws IOException, JsonProcessingException {
                try {
                    gen.writeBinary(value.getBinaryStream(), -1);
                } catch (final SQLException e) {
                    throw new IOException(e);
                }
            }

            @Override
            public Class<Blob> handledType() {
                return Blob.class;
            }
        });
        objectMapper.registerModule(m);
    }

    private static void createDynamoDBTable(final DynamoDBConfiguration config,
            final DynamoDBClient dynamoDBClient) {
        final ProvisionedThroughput throughput = config.buildThroughput();
        try {
            dynamoDBClient.createTable(throughput);
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

    private void setupNuxeoClient(final HarvestServiceConfiguration conf,
            final Environment env, final DBI dbi) throws Exception {
        final NuxeoClientConfiguration nxconf =
                conf.getNuxeoClientConfiguration();
        if (nxconf == null) {
            LOGGER.warn("Nuxeo client not configured. Repository information " +
                 "will not be read from Nuxeo.");
            return;
        }
        if (!nxconf.isScheduleEnabled()) {
            LOGGER.warn("Scheduled repository updates are disabled.");
            return;
        }
        nxconf.schedule(env, dbi);
    }

    private void setupDynamoDBMonitor(final Environment env,
            final DynamoDBConfiguration config, final DynamoDBClient client,
            final JobManager jobManager) {
        if (! config.isMonitorConfigured()) {
            LOGGER.warn("DynamoDB write capacity monitor is not configured. " +
                    "Cannot automatically adjust write capacity.");
            return;
        }
        config.scheduleMonitor(env, client, jobManager);
    }

    private JIRAClient jiraClient(final Environment env,
            final HarvestServiceConfiguration conf) {
        final JIRAClientConfiguration jiraConfig =
                conf.getJIRAClientConfiguration();
        if (jiraConfig == null) {
            LOGGER.warn("JIRA client not configured. Issues will not be " +
                    "created for failed harvests.");
            return null;
        }
        return jiraConfig.build(env);
    }

    private Consumer<HarvestNotification> failureListener(final Environment env,
            final HarvestServiceConfiguration conf) {
        final JIRAClient jiraClient = jiraClient(env, conf);
        return (jiraClient == null) ? x -> {} : new FailureListener(jiraClient);
    }


    @Override
    public void run(final HarvestServiceConfiguration conf,
            final Environment env) throws Exception {
        final DBI dbi = new DBIFactory().build(env,
                conf.getDataSourceFactory(), DBI_NAME);
        final HttpClient httpClient = new HarvestHttpClientBuilder(env)
                .using(conf.getHttpClientConfiguration())
                .build(HTTP_CLIENT_NAME);
        final HarvestJobConfiguration jobConfig = conf.getJobConfiguration();
        final ExecutorService executor = jobConfig.executorService(env);
        final DynamoDBConfiguration dynamoDBConfig =
                conf.getDynamoDBConfiguration();
        final DynamoDBClient dynamoDBClient = dynamoDBConfig.buildClient();
        createDynamoDBTable(dynamoDBConfig, dynamoDBClient);
        setupNuxeoClient(conf, env, dbi);
        startH2Servers(conf, env);
        final JobManager jobManager = new JobManager(jobConfig, httpClient,
                dynamoDBClient, dbi, failureListener(env, conf));
        setupDynamoDBMonitor(env, dynamoDBConfig, dynamoDBClient, jobManager);
        final JobResource jr = new JobResource(dbi, jobManager, executor);
        env.jersey().register(jr);
    }

    public static void main(final String[] args) throws Exception {
        new HarvestServiceApplication().run(args);
    }
}
