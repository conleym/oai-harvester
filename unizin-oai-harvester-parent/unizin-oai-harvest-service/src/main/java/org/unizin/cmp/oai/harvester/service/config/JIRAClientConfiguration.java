package org.unizin.cmp.oai.harvester.service.config;

import java.net.URI;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.http.client.HttpClient;
import org.unizin.cmp.oai.harvester.service.client.JIRAClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Environment;

public final class JIRAClientConfiguration {

    @JsonProperty
    @NotNull
    private URI endpoint;

    @JsonProperty
    @NotNull
    private String username;

    @JsonProperty
    @NotNull
    private String password;

    @JsonProperty("httpClient")
    @Nonnull
    @Valid
    private HarvestHttpClientConfiguration httpClient =
        new HarvestHttpClientConfiguration();

    private HttpClient buildHttpClient(final Environment env) {
        return new HttpClientBuilder(env).using(httpClient)
                .build("JIRA");
    }

    public JIRAClient build(final Environment env) {
        return new JIRAClient(endpoint, buildHttpClient(env), username,
                password, env.getObjectMapper());
    }
}
