package org.unizin.cmp.harvester.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;


public final class HarvestServiceConfiguration extends Configuration {
    @JsonProperty("database")
    private DataSourceFactory dsFactory;

    public DataSourceFactory getDataSourceFactory() {
        return dsFactory;
    }
}
