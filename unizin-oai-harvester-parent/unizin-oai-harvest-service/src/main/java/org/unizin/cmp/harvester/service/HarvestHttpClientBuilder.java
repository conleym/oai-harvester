package org.unizin.cmp.harvester.service;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.httpclient.InstrumentedHttpClientConnectionManager;

import io.dropwizard.client.ConfiguredCloseableHttpClient;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.setup.Environment;

public final class HarvestHttpClientBuilder extends HttpClientBuilder {
    private HttpClientConfiguration config;

    public HarvestHttpClientBuilder(final Environment environment) {
        super(environment);
    }

    public HarvestHttpClientBuilder(final MetricRegistry metricRegistry) {
        super(metricRegistry);
    }

    @Override
    public HarvestHttpClientBuilder using(final HttpClientConfiguration config) {
        this.config = config;
        super.using(config);
        return this;
    }

    @Override
    protected ConfiguredCloseableHttpClient createClient(
            final org.apache.http.impl.client.HttpClientBuilder builder,
            final InstrumentedHttpClientConnectionManager manager,
            final String name) {
        if (config instanceof HarvestHttpClientConfiguration) {
            final HarvestHttpClientConfiguration harvestConfig =
                    (HarvestHttpClientConfiguration)config;
            builder.setDefaultHeaders(harvestConfig.getDefaultHeaders());
        }
        return super.createClient(builder, manager, name);
    }
}
