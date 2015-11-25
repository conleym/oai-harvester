package org.unizin.cmp.harvester.service;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.unizin.cmp.harvester.job.HarvestJob;
import org.unizin.cmp.harvester.job.HarvestedOAIRecord;
import org.unizin.cmp.harvester.service.config.HarvestJobConfiguration;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.harvester.HarvestParams;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;

@Path("/job")
@Produces(MediaType.APPLICATION_JSON)
public final class JobResource {

    private final DataSource ds;
    private final HarvestJobConfiguration jobConfig;
    private final HttpClient httpClient;
    private final DynamoDBMapper mapper;
    private final ExecutorService executor;

    public JobResource(final DataSource ds,
            final HarvestJobConfiguration jobConfig,
            final HttpClient httpClient,
            final DynamoDBMapper mapper,
            final ExecutorService executor) {
        this.ds = ds;
        this.jobConfig = jobConfig;
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.executor = executor;
    }

    @Path("/dynamoCount")
    @GET
    public int countRecords() {
        return mapper.count(HarvestedOAIRecord.class, new DynamoDBScanExpression());
    }

    @Path("/listRecords")
    @POST
    public Map<String, Object> createJob(
            @NotNull @QueryParam("baseURI") final URI uri,
            @QueryParam("metadataPrefix") final String metadataPrefix,
            @QueryParam("set") final String set,
            @QueryParam("from") final String from,
            @QueryParam("until") final String until)
                    throws NoSuchAlgorithmException {
        final HarvestParams.Builder builder = new HarvestParams.Builder(uri,
                OAIVerb.LIST_RECORDS);
        if (metadataPrefix != null) {
            builder.withMetadataPrefix(metadataPrefix);
        }
        if (from != null) {
            builder.withFrom(from);
        }
        if (until != null) {
            builder.withUntil(until);
        }
        if (set != null) {
            builder.withSet(set);
        }
        if (!builder.isValid()) {
            throw new WebApplicationException(HttpStatus.SC_BAD_REQUEST);
        }
        final HarvestJob job = jobConfig.buildJob(httpClient, mapper, executor,
                Collections.singletonList(builder.build()),
                Collections.emptyList());
        executor.submit(job::start);
        return Collections.emptyMap();
    }


    @GET
    public Map<String, Object> status() {
        return new HashMap<String, Object>(){{
            put("HELLO", 27);
        }};
    }
}
