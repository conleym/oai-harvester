package org.unizin.cmp.oai.harvester.service.config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.db.DataSourceFactory;


public final class HarvestServiceConfiguration extends Configuration {
    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory dsFactory;

    @Valid
    @NotNull
    @JsonProperty("job")
    private HarvestJobConfiguration jobFactory;

    @Valid
    @NotNull
    @JsonProperty("httpClient")
    private HarvestHttpClientConfiguration httpClient;

    @Valid
    @NotNull
    @JsonProperty("dynamoDB")
    private DynamoDBConfiguration dynamoDB;

    @Valid
    @JsonProperty("h2Server")
    private H2ServerConfiguration h2Server = new H2ServerConfiguration();


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

    public H2ServerConfiguration getH2ServerConfiguration() {
        return h2Server;
    }
}
