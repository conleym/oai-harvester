package org.unizin.cmp.oai.harvester.service;

import java.util.function.Supplier;

import org.skife.jdbi.v2.DBI;
import org.unizin.cmp.oai.harvester.service.config.HarvestServiceConfiguration;

import io.dropwizard.testing.junit.DropwizardAppRule;

/**
 * Lazily get a DBI instance from dropwizard application rule.
 * <p>
 * It seems that there's some setup involved inside this rule, and that the
 * database configuration is {@code null} before this runs, so we need to get it
 * from within test and setup methods. But we'd like to get it just once and
 * store it as an instance variable on the test class. This lets us do both.
 * </p>
 */
public final class LazyDBI implements Supplier<DBI> {
    private final DropwizardAppRule<HarvestServiceConfiguration> app;
    private DBI dbi;

    public LazyDBI(final DropwizardAppRule<HarvestServiceConfiguration> app) {
        this.app = app;
    }

    @Override
    public DBI get() {
        if (dbi == null) {
            dbi = ServiceTests.dbi(app);
        }
        return dbi;
    }
}
