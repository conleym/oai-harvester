package org.unizin.cmp.oai.harvester.service.config;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Nonnull;
import javax.validation.Valid;

import org.apache.http.client.HttpClient;
import org.unizin.cmp.oai.harvester.service.NuxeoClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Environment;


public final class NuxeoClientConfiguration {
    private static final String NAME = "nuxeo-client";

    @JsonProperty
    @Nonnull
    private URI nuxeoURI;

    @JsonProperty
    private String password;

    @JsonProperty
    private String user;

    @JsonProperty
    private int pageSize = 20;

    /** Amount of time between nuxeo client runs. */
    @JsonProperty
    @Nonnull
    private Duration period = Duration.ofMinutes(5);

    @JsonProperty("httpClient")
    @Nonnull
    @Valid
    private HarvestHttpClientConfiguration httpClient =
        new HarvestHttpClientConfiguration();


    public NuxeoClient client(final Environment env,
            final ObjectMapper mapper) {
        final HttpClientBuilder builder = new HarvestHttpClientBuilder(env)
                .using(httpClient);
        final HttpClient httpClient = builder.build(NAME);
        return new NuxeoClient(mapper, httpClient, nuxeoURI, user, password,
                pageSize);
    }

    public ScheduledExecutorService executorService(final Environment env) {
        return env.lifecycle().scheduledExecutorService(NAME + "-%s")
                .threads(1)
                .build();
    }

    public long getPeriodMillis() {
        return period.toMillis();
    }
}
