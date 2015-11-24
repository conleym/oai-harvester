package org.unizin.cmp.harvester.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.db.DataSourceFactory;


public final class HarvestServiceConfiguration extends Configuration {
    @JsonProperty("database")
    private DataSourceFactory dsFactory;

    @JsonProperty("job")
    private HarvestJobFactory jobFactory;

    @JsonProperty("httpClient")
    private HarvestHttpClientConfiguration httpClient;

    public DataSourceFactory getDataSourceFactory() {
        return dsFactory;
    }

    public HarvestJobFactory getJobFactory() {
        return jobFactory;
    }

    public HttpClientConfiguration getHttpClientConfiguration() {
        return httpClient;
    }
}
