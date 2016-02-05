package org.unizin.cmp.oai.harvester.service.config;

import java.net.URI;
import java.time.Duration;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.harvester.job.HarvestedOAIRecord;
import org.unizin.cmp.oai.harvester.service.DynamoDBClient;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.ConsistentReads;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DynamoDBConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            DynamoDBConfiguration.class);

    public static final class DynamoDBMapperConfiguration {
        @JsonProperty
        private String tableNameOverride;

        @JsonProperty
        @NotNull
        private ConsistentReads consistentReads = ConsistentReads.CONSISTENT;

        public DynamoDBMapper build(final AmazonDynamoDB client) {
            final DynamoDBMapperConfig.Builder config =
                    new DynamoDBMapperConfig.Builder()
                    .withConsistentReads(consistentReads);
            if (tableNameOverride != null) {
                config.withTableNameOverride(new TableNameOverride(
                        tableNameOverride));
            }
            return new DynamoDBMapper(client, config.build());
        }

        boolean hasTableNameOverride() {
            return tableNameOverride != null &&
                    !"".equals(tableNameOverride.trim());
        }
    }

    @JsonProperty
    private URI endpoint;

    @JsonProperty
    private String region;

    @JsonProperty
    private String awsAccessKey;

    @JsonProperty
    private String awsAccessKeyID;

    @JsonProperty
    @Min(1)
    private long provisionedReadCapacity = 1;

    @JsonProperty
    @Min(1)
    private long provisionedWriteCapacity = 1;

    @JsonProperty
    private Duration capacityAdjustmentInterval = Duration.ofSeconds(20);

    /**
     * If the maximum queue size of all jobs exceeds this number, we increase
     * the DynamoDB write capacity, provided it's not already at the configured
     * {@link #maxWriteCapacity maximum}.
     */
    @JsonProperty
    @Min(1)
    private Long increaseCapacityThreshold;

    @JsonProperty
    @Min(1)
    private Long decreaseCapacityThreshold;

    @JsonProperty
    @Min(100)
    private long maxWriteCapacity = 1200;

    @JsonProperty
    @Min(1)
    private long minWriteCapacity = 1;

    @JsonProperty
    @Valid
    @NotNull
    private DynamoDBMapperConfiguration recordMapper =
        new DynamoDBMapperConfiguration();


    private AmazonDynamoDB buildDDB() {
        AWSCredentialsProvider provider = null;
        if (awsAccessKeyID == null || awsAccessKey == null) {
            LOGGER.info("Secret keys not defined in configuration. Trying" +
                    " default provider chain.");
            // Set up defaults.
            provider = new DefaultAWSCredentialsProviderChain();
        } else {
            provider = new StaticCredentialsProvider(
                        new BasicAWSCredentials(awsAccessKeyID, awsAccessKey));
        }
        final AmazonDynamoDBClient db = new AmazonDynamoDBClient(provider);
        if (region != null) {
            db.withRegion(Regions.fromName(region));
        }
        if (endpoint != null) {
            db.setEndpoint(endpoint.toString());
        }
        return db;
    }

    public ProvisionedThroughput buildThroughput() {
        return new ProvisionedThroughput(provisionedReadCapacity,
                provisionedWriteCapacity);
    }

    public DynamoDBClient build() {
        final AmazonDynamoDB ddb = buildDDB();
        final DynamoDBMapper mapper = recordMapper.build(ddb);
        final String tableName = recordMapper.hasTableNameOverride() ?
                recordMapper.tableNameOverride : HarvestedOAIRecord.TABLE_NAME;
        return new DynamoDBClient(tableName, ddb, mapper);
    }

    public Long getIncreaseCapacityThreshold() {
        return increaseCapacityThreshold;
    }

    public Long getDecreaseCapacityThreshold() {
        return decreaseCapacityThreshold;
    }

    public long getMaxCapacity() {
        return maxWriteCapacity;
    }

    public long getMinCapacity() {
        return minWriteCapacity;
    }

    public Duration getCapacityAdjustmentInterval() {
        return capacityAdjustmentInterval;
    }
}
