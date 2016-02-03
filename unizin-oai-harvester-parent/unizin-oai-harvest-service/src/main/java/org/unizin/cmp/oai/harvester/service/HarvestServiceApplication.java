package org.unizin.cmp.oai.harvester.service;

import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.skife.jdbi.v2.Call;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.harvester.job.HarvestedOAIRecord;
import org.unizin.cmp.oai.harvester.service.config.DynamoDBConfiguration;
import org.unizin.cmp.oai.harvester.service.config.HarvestHttpClientBuilder;
import org.unizin.cmp.oai.harvester.service.config.HarvestServiceConfiguration;
import org.unizin.cmp.oai.harvester.service.config.NuxeoClientConfiguration;
import org.unizin.cmp.oai.harvester.service.db.DBIUtils;
import org.unizin.cmp.oai.harvester.service.db.ManagedH2Server;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.StreamSpecification;
import com.amazonaws.services.dynamodbv2.model.StreamViewType;
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

    private void addRepo(final DBI dbi, final Map<String, Object> entry,
            final List<String> names, final List<String> baseURIs,
            final List<String> institutions) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> props =
                (Map<String, Object>)entry.get("properties");
        names.add((String)props.get("dc:title"));
        baseURIs.add((String)props.get("repo:baseUrl"));
        institutions.add((String)props.get("repo:owner"));
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
        final NuxeoClient client = nxconf.client(env, objectMapper);
        final ScheduledExecutorService ses = nxconf.executorService(env);
        final Runnable r = () -> {
            try {
                LOGGER.info("Getting repositories from Nuxeo.");
                // Accumulate all the info we're interested in, then call the
                // update function.
                final List<String> names = new ArrayList<>();
                final List<String> uris = new ArrayList<>();
                final List<String> institutions = new ArrayList<>();
                client.repositories().forEach(page -> {
                    @SuppressWarnings("unchecked")
                    final List<Map<String, Object>> entries =
                            (List<Map<String, Object>>)page.get("entries");
                    entries.forEach(entry -> {
                        addRepo(dbi, entry, names, uris, institutions);
                    });
                });
                try (final Handle h = DBIUtils.handle(dbi)) {
                    final Call c = h.createCall(
                            "call UPDATE_REPOSITORIES(#names, #uris, " +
                            "#institutions)");
                    c.bind("names", names)
                        .bind("uris", uris)
                        .bind("institutions", institutions);
                    c.invoke();
                }
                LOGGER.info("Done updating repositories.");
            } catch (final Exception e) {
                /* Uncaught exceptions will cause the scheduler to stop running
                 * this task, so catch them all. */
                LOGGER.error("Error updating repositories from Nuxeo.", e);
            }
        };
        ses.scheduleAtFixedRate(r, 0, nxconf.getPeriodMillis(),
                TimeUnit.MILLISECONDS);
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
                .executorService(env);
        final DynamoDBConfiguration dynamo = conf.getDynamoDBConfiguration();
        createMapper(dynamo);
        createDynamoDBTable(dynamo);
        setupNuxeoClient(conf, env, jdbi);
        startH2Servers(conf, env);
        final JobResource jr = new JobResource(jdbi,
                conf.getJobConfiguration(), httpClient, dynamoDBMapper,
                executor);
        env.jersey().register(jr);
    }

    public static void main(final String[] args) throws Exception {
        new HarvestServiceApplication().run(args);
    }
}
