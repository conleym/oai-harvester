package org.unizin.cmp.oai.harvester.service;

import java.net.URI;
import java.net.URISyntaxException;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.unizin.cmp.oai.harvester.job.Tests;
import org.unizin.cmp.oai.harvester.service.config.HarvestServiceConfiguration;

import io.dropwizard.java8.jdbi.DBIFactory;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;

public final class ServiceTests {
    public static final URI MOCK_NUXEO_URI;
    static {
        try {
            MOCK_NUXEO_URI = new URI(String.format(
                    "http://localhost:%d/nuxeoAPI", Tests.WIREMOCK_PORT));
        } catch (final URISyntaxException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final String DEFAULT_DROPWIZARD_PORT = "9999";
    public static final String DROPWIZARD_PORT;
    static {
        final String port = System.getProperty("dropwizard.port");
        DROPWIZARD_PORT = (port == null || "".equals(port.trim())) ?
                DEFAULT_DROPWIZARD_PORT : port;
    }

    public static final String TEST_CONFIG_PATH = ResourceHelpers
            .resourceFilePath("test-config.yml");

    public static DropwizardAppRule<HarvestServiceConfiguration> newAppRule() {
        return new DropwizardAppRule<HarvestServiceConfiguration>(
                HarvestServiceApplication.class, TEST_CONFIG_PATH,
                ConfigOverride.config("nuxeoClient.nuxeoURI",
                        MOCK_NUXEO_URI.toString()),
                ConfigOverride.config("dynamoDB.endpoint",
                        String.format("http://localhost:%s",
                                Tests.DYNAMO_PORT)),
                ConfigOverride.config("server.applicationConnectors[0].port",
                        DROPWIZARD_PORT));
    }

    public static void migrateDatabase(
            final DropwizardAppRule<HarvestServiceConfiguration> app)
                    throws Exception {
        app.getApplication().run("db", "migrate", TEST_CONFIG_PATH);
    }

    public static void dropDatabaseObjects(final DBI dbi) {
        try (final Handle h = dbi.open()) {
            h.createStatement("drop all objects").execute();
        }
    }

    public static DBI dbi(
            final DropwizardAppRule<HarvestServiceConfiguration> app) {
        return new DBIFactory().build(app.getEnvironment(),
                app.getConfiguration().getDataSourceFactory(), "test");
    }

    /** No instances allowed. */
    private ServiceTests() { }
}
