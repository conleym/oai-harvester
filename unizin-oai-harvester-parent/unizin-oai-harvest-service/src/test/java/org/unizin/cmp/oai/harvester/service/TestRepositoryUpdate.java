package org.unizin.cmp.oai.harvester.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.skife.jdbi.v2.Handle;
import org.unizin.cmp.oai.harvester.job.Tests;
import org.unizin.cmp.oai.harvester.service.config.HarvestServiceConfiguration;
import org.unizin.cmp.oai.harvester.service.db.DBIUtils;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;

import io.dropwizard.testing.junit.DropwizardAppRule;


public final class TestRepositoryUpdate {
    @Rule
    public final WireMockRule wireMock = Tests.newWireMockRule();

    /** Starts the application only once. */
    @ClassRule
    public static final DropwizardAppRule<HarvestServiceConfiguration> app =
    ServiceTests.newAppRule();

    private static final LazyApp lazy = new LazyApp(app);


    private static final Map<String, String> repoMap(final String baseURI,
            final String name, final String institution) {
        return ImmutableMap.of(
                "REPOSITORY_BASE_URI", baseURI,
                "REPOSITORY_NAME", name,
                "REPOSITORY_INSTITUTION", institution);
    }

    private static final String REPO_UPDATE_SCENARIO = "repo update";
    private static final String IU_BASE_URI =
            "http://scholarworks.iu.edu/dspace-oai/request";
    private static final Collection<String> IGNORED_KEYS = Arrays.asList(
            "REPOSITORY_ID", "REPOSITORY_ENABLED");
    private static final Map<String, String> IU_EXPECTED = repoMap(
            "http://scholarworks.iu.edu/dspace-oai/request",
            "IUScholarWorks Repository", "Indiana University");
    private static final Map<String, String> OREGON_EXPECTED = repoMap(
            "http://ir.library.oregonstate.edu/oai/request",
            "ScholarsArchive@OSU", "Oregon State University");
    private static final Map<String, String> IOWA_EXPECTED = repoMap(
            "http://ir.uiowa.edu/do/oai/", "Iowa Research Online",
            "University of Iowa");
    private static final Map<String, Map<String, String>> EXPECTED_BY_URI =
            ImmutableMap.of(
                    baseURI(IU_EXPECTED), IU_EXPECTED,
                    baseURI(OREGON_EXPECTED), OREGON_EXPECTED,
                    baseURI(IOWA_EXPECTED), IOWA_EXPECTED);

    private static List<Map<String, Object>> readRepositories() {
        try (final Handle h = DBIUtils.handle(lazy.dbi())) {
            return h.createQuery("select * from REPOSITORY").list();
        }
    }

    private static Boolean enabled(final Map<String, ?> repo) {
        return (Boolean)repo.get("REPOSITORY_ENABLED");
    }

    private static String baseURI(final Map<String, ?> repo) {
        return (String)repo.get("REPOSITORY_BASE_URI");
    }

    private static String name(final Map<String, ?> repo) {
        return (String)repo.get("REPOSITORY_NAME");
    }

    @Before
    public void createDB() throws Exception {
        ServiceTests.migrateDatabase(app);
        try (final Handle h = DBIUtils.handle(lazy.dbi())) {
            h.createStatement("delete from REPOSITORY").execute();
        }
    }

    @After
    public void destroyDB() {
        ServiceTests.dropDatabaseObjects(lazy.dbi());
    }

    private static void compareResults(final Map<String, Object> map) {
        final Map<String, Object> comparison = new HashMap<>();
        map.entrySet().stream().filter(
                entry -> !IGNORED_KEYS.contains(
                        entry.getKey().toUpperCase()))
        .forEach(
                entry -> comparison.put(entry.getKey().toUpperCase(),
                        entry.getValue()));
        final String baseURI = baseURI(comparison);
        Assert.assertEquals(comparison, EXPECTED_BY_URI.get(baseURI));
    }

    private void runWithInitialExpectations() {
        lazy.repositoryUpdater().update();
        final List<Map<String, Object>> repos = readRepositories();
        Assert.assertEquals(2, repos.size());
        repos.forEach(map -> {
            compareResults(map);
            Assert.assertTrue("New repository should be enabled.",
                    enabled(map));
        });
    }

    @Test
    public void testRepositoryUpdate() throws Exception {
        stubFor(get(urlPathEqualTo(ServiceTests.MOCK_NUXEO_URI.getPath()))
                .inScenario(REPO_UPDATE_SCENARIO)
                .willSetStateTo("2")
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBodyFile("nuxeo/first-nuxeo-response.json")));

        stubFor(get(urlPathEqualTo(ServiceTests.MOCK_NUXEO_URI.getPath()))
                .inScenario(REPO_UPDATE_SCENARIO)
                .whenScenarioStateIs("2")
                .willSetStateTo("3")
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBodyFile("nuxeo/second-nuxeo-response.json")));

        stubFor(get(urlPathEqualTo(ServiceTests.MOCK_NUXEO_URI.getPath()))
                .inScenario(REPO_UPDATE_SCENARIO)
                .whenScenarioStateIs("3")
                .willSetStateTo("4")
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBodyFile("nuxeo/first-nuxeo-response.json")));

        stubFor(get(urlPathEqualTo(ServiceTests.MOCK_NUXEO_URI.getPath()))
                .inScenario(REPO_UPDATE_SCENARIO)
                .whenScenarioStateIs("4")
                .willSetStateTo("5")
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBodyFile("nuxeo/third-nuxeo-response.json")));

        runWithInitialExpectations();

        lazy.repositoryUpdater().update();
        List<Map<String, Object>> repos = readRepositories();
        Assert.assertEquals(2, repos.size());
        repos.forEach(map -> {
            final String baseURI = baseURI(map);
            if (IU_BASE_URI.equals(baseURI)) {
                Assert.assertEquals("Indiana University 2",
                        map.get("REPOSITORY_INSTITUTION"));
                Assert.assertEquals(name(IU_EXPECTED), name(map));
                Assert.assertTrue("IU should be enabled.", enabled(map));
            } else {
                compareResults(map);
                Assert.assertFalse("Oregon should be disabled.", enabled(map));
            }
        });

        runWithInitialExpectations();

        lazy.repositoryUpdater().update();
        repos = readRepositories();
        Assert.assertEquals(3, repos.size());
        repos.forEach(map -> {
            compareResults(map);
            Assert.assertTrue("Repository should be enabled.", enabled(map));
        });
    }

    /**
     * This is just here to ensure that more tests could be written if needed.
     * <p></p>
     */
    @Test
    public void testZ() throws Exception {
        lazy.repositoryUpdater().run();
    }
}
