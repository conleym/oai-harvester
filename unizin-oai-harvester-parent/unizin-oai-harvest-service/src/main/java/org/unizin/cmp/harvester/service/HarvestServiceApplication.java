package org.unizin.cmp.harvester.service;

import javax.sql.DataSource;

import org.apache.http.client.HttpClient;
import org.skife.jdbi.v2.DBI;
import org.unizin.cmp.harvester.service.config.HarvestHttpClientBuilder;
import org.unizin.cmp.harvester.service.config.HarvestServiceConfiguration;

import com.codahale.metrics.MetricRegistry;

import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.jdbi.DBIHealthCheck;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public final class HarvestServiceApplication
extends Application<HarvestServiceConfiguration> {
    private static final String CONNECTION_POOL_NAME =
            "HarvestService Database Connection Pool";
    private static final String HTTP_CLIENT_NAME =
            "HarvestService HTTP Client";

    private Bootstrap<HarvestServiceConfiguration> bootstrap;

    @Override
    public void initialize(final Bootstrap<HarvestServiceConfiguration> bootstrap) {
        super.initialize(bootstrap);
        this.bootstrap = bootstrap;
    }

    private DataSource createConnectionPool(
            final HarvestServiceConfiguration configuration,
            final Environment environment) {
        final DataSourceFactory dsf = configuration.getDataSourceFactory();
        final MetricRegistry mr = bootstrap.getMetricRegistry();
        final ManagedDataSource ds = dsf.build(mr, CONNECTION_POOL_NAME);
        environment.lifecycle().manage(ds);
        environment.healthChecks().register(CONNECTION_POOL_NAME,
                new DBIHealthCheck(new DBI(ds), dsf.getValidationQuery()));
        return ds;
    }

    @Override
    public void run(final HarvestServiceConfiguration conf,
            final Environment env) throws Exception {
        final DataSource ds = createConnectionPool(conf, env);
        final HttpClient httpClient = new HarvestHttpClientBuilder(env)
                .using(conf.getHttpClientConfiguration())
                .build(HTTP_CLIENT_NAME);
        env.jersey().register(new JobResource(ds, conf.getJobFactory(),
                httpClient));
    }

    public static void main(final String[] args) throws Exception {
        new HarvestServiceApplication().run(args);
    }
}
