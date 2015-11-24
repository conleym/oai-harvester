package org.unizin.cmp.harvester.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.client.HttpClientConfiguration;

public final class HarvestHttpClientConfiguration extends HttpClientConfiguration {

    @JsonProperty("defaultHeaders")
    private Collection<? extends Header> defaultHeaders;

    @JsonProperty("defaultHeaders")
    public void setDefaultHeaders(final Map<String, String> map) {
        final List<BasicHeader> headers = map.entrySet().stream()
            .map((e) -> new BasicHeader(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
        defaultHeaders = Collections.unmodifiableList(headers);
    }

    public Collection<? extends Header> getDefaultHeaders() {
        return defaultHeaders;
    }
}
