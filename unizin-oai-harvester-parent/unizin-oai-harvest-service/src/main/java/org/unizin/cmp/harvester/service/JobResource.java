package org.unizin.cmp.harvester.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.http.client.HttpClient;
import org.unizin.cmp.harvester.service.config.HarvestJobFactory;

@Path("/job")
@Produces(MediaType.APPLICATION_JSON)
public final class JobResource {

    private final DataSource ds;
    private final HarvestJobFactory factory;
    private final HttpClient httpClient;

    public JobResource(final DataSource ds,
            final HarvestJobFactory factory,
            final HttpClient httpClient) {
        this.ds = ds;
        this.factory = factory;
        this.httpClient = httpClient;
    }

    @POST
    public Map<String, Object> createJob() {
        return Collections.emptyMap();
    }

    @GET
    public Map<String, Object> getStatus() {
        return new HashMap<String, Object>(){{
            put("HELLO", 27);
        }};
    }
}
