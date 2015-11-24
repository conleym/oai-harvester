package org.unizin.cmp.harvester.service.config;

import java.net.URI;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.ConsistentReads;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride;
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
    }


    @JsonProperty
    private URI endpoint;

    @JsonProperty
    private String awsSecretKey;

    @JsonProperty
    private String awsSecretKeyID;

    @JsonProperty
    @Valid
    private DynamoDBMapperConfiguration recordMapper;

    public AmazonDynamoDB build() {
        if (awsSecretKeyID == null || awsSecretKey == null) {
            LOGGER.info("Secret keys not defined in configuration. Trying" +
                    " default provider chain.");
            // Set up defaults.
            final DefaultAWSCredentialsProviderChain chain
                = new DefaultAWSCredentialsProviderChain();
            final AWSCredentials defaultCreds = chain.getCredentials();
            awsSecretKey = defaultCreds.getAWSSecretKey();
            awsSecretKeyID = defaultCreds.getAWSAccessKeyId();
        }
        final AWSCredentials credentials = new BasicAWSCredentials(awsSecretKeyID,
                awsSecretKey);
        final AmazonDynamoDB db = new AmazonDynamoDBClient(credentials);
        db.setEndpoint(endpoint.toString());
        return db;
    }

    public DynamoDBMapperConfiguration getRecordMapperConfiguration() {
        return recordMapper;
    }
}
