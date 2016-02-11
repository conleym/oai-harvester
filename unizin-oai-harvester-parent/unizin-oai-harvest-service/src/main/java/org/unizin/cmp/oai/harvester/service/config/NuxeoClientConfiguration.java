package org.unizin.cmp.oai.harvester.service.config;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.validation.Valid;

import org.apache.http.client.HttpClient;
import org.skife.jdbi.v2.DBI;
import org.unizin.cmp.oai.harvester.service.RepositoryUpdater;
import org.unizin.cmp.oai.harvester.service.client.NuxeoClient;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    @JsonProperty
    private boolean scheduleEnabled = true;

    @JsonProperty("httpClient")
    @Nonnull
    @Valid
    private HarvestHttpClientConfiguration httpClient =
        new HarvestHttpClientConfiguration();


    public NuxeoClient client(final Environment env) {
        final HttpClientBuilder builder = new HarvestHttpClientBuilder(env)
                .using(httpClient);
        final HttpClient httpClient = builder.build(NAME);
        return new NuxeoClient(env.getObjectMapper(), httpClient, nuxeoURI,
                user, password, pageSize);
    }

    private ScheduledExecutorService executorService(final Environment env) {
        return env.lifecycle().scheduledExecutorService(NAME + "-%s")
                .threads(1)
                .build();
    }

    public void schedule(final Environment env, final DBI dbi) {
        final NuxeoClient nxClient = client(env);
        final ScheduledExecutorService ses = executorService(env);
        ses.scheduleAtFixedRate(new RepositoryUpdater(nxClient, dbi), 0,
                period.toMillis(), TimeUnit.MILLISECONDS);
    }

    public boolean isScheduleEnabled() {
        return scheduleEnabled;
    }
}
