package org.unizin.cmp.harvester.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/job")
@Produces(MediaType.APPLICATION_JSON)
public final class JobResource {

    private final DataSource ds;

    public JobResource(final DataSource ds) {
        this.ds = ds;
    }

    @PUT
    public Map<String, Object> put() {
        return Collections.emptyMap();
    }

    @GET
    public Map<String, Object> get() {
        return new HashMap<String, Object>(){{
            put("HELLO", 27);
        }};
    }
}
