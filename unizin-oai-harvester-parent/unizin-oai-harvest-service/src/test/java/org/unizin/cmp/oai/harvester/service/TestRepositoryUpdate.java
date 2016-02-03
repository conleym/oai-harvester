package org.unizin.cmp.oai.harvester.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.unizin.cmp.oai.harvester.job.Tests;
import org.unizin.cmp.oai.harvester.service.config.HarvestServiceConfiguration;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.dropwizard.testing.junit.DropwizardAppRule;


public final class TestRepositoryUpdate {
    @Rule
    public final WireMockRule wireMock = Tests.newWireMockRule();

    @Rule
    public final DropwizardAppRule<HarvestServiceConfiguration> app =
        ServiceTests.newAppRule();


    private final LazyDBI dbi = new LazyDBI(app);

    @Before
    public void createDB() throws Exception {
        ServiceTests.migrateDatabase(app);
    }

    @After
    public void destroyDB() {
        ServiceTests.dropDatabaseObjects(dbi.get());
    }

    @Test
    public void test1() throws Exception {
        System.out.println("HELLO");
    }

    @Test
    public void test2() throws Exception {
        System.out.println("GOODBYE");
    }
}
