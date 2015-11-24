package org.unizin.cmp.harvester.service.config;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.db.DataSourceFactory;


public final class HarvestServiceConfiguration extends Configuration {
    @Valid
    @JsonProperty("database")
    private DataSourceFactory dsFactory;

    @Valid
    @JsonProperty("job")
    private HarvestJobConfiguration jobFactory;

    @Valid
    @JsonProperty("httpClient")
    private HarvestHttpClientConfiguration httpClient;

    @Valid
    @JsonProperty("dynamoDB")
    private DynamoDBConfiguration dynamoDB;


    public DataSourceFactory getDataSourceFactory() {
        return dsFactory;
    }

    public HarvestJobConfiguration getJobConfiguration() {
        return jobFactory;
    }

    public HttpClientConfiguration getHttpClientConfiguration() {
        return httpClient;
    }

    public DynamoDBConfiguration getDynamoDBConfiguration() {
        return dynamoDB;
    }
}
