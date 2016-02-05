package org.unizin.cmp.oai.harvester.service;

import org.skife.jdbi.v2.DBI;
import org.unizin.cmp.oai.harvester.service.config.HarvestServiceConfiguration;

import io.dropwizard.testing.junit.DropwizardAppRule;

/**
 * Lazily get various objects from dropwizard application rule.
 * <p>
 * It seems that there's some setup involved inside the rule, and that
 * configurations can be {@code null} if we try to access them outside of a
 * test. The application itself takes a bit of time to start up, and some
 * objects can be created only once (due to metrics registry constraints, for
 * example) or are relatively expensive to create. This gives us access to those
 * objects after the setup without forcing us to recreate them every time we
 * want access.
 * </p>
 */
public final class LazyApp {
    private final DropwizardAppRule<HarvestServiceConfiguration> app;
    private DBI dbi;
    private NuxeoClient nuxeoClient;
    private RepositoryUpdater repositoryUpdater;

    public LazyApp(final DropwizardAppRule<HarvestServiceConfiguration> app) {
        this.app = app;
    }

    public DBI dbi() {
        if (dbi == null) {
            dbi = ServiceTests.dbi(app);
        }
        return dbi;
    }

    public NuxeoClient nuxeoClient() {
        if (nuxeoClient == null) {
            nuxeoClient = app.getConfiguration().getNuxeoClientConfiguration()
                    .client(app.getEnvironment());
        }
        return nuxeoClient;
    }

    public RepositoryUpdater repositoryUpdater() {
        if (repositoryUpdater == null) {
            repositoryUpdater = new RepositoryUpdater(nuxeoClient(), dbi());
        }
        return repositoryUpdater;
    }
}
