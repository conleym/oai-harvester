package org.unizin.cmp.oai.harvester.service;

import org.unizin.cmp.oai.harvester.job.HarvestedOAIRecord;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.StreamSpecification;
import com.amazonaws.services.dynamodbv2.model.StreamViewType;

public class DynamoDBClient {

    private final String tableName;
    private final AmazonDynamoDB dynamoDB;
    private final DynamoDBMapper mapper;

    public DynamoDBClient(final String tableName,
            final AmazonDynamoDB dynamoDB, final DynamoDBMapper mapper) {
        this.tableName = tableName;
        this.dynamoDB = dynamoDB;
        this.mapper = mapper;
    }

    public void createTable(final ProvisionedThroughput throughput) {
        final StreamSpecification streamSpec = new StreamSpecification()
                .withStreamEnabled(true)
                .withStreamViewType(StreamViewType.NEW_AND_OLD_IMAGES);
        final CreateTableRequest req = mapper
                .generateCreateTableRequest(HarvestedOAIRecord.class)
                .withProvisionedThroughput(throughput)
                .withStreamSpecification(streamSpec);
        dynamoDB.createTable(req);
    }

    public void setWriteCapacity(final long writeCapacity) {
        final ProvisionedThroughput throughput = new ProvisionedThroughput()
                .withWriteCapacityUnits(writeCapacity);
        dynamoDB.updateTable(tableName, throughput);
    }

    public long getWriteCapacity() {
        return dynamoDB.describeTable(tableName)
                .getTable().getProvisionedThroughput().getWriteCapacityUnits();
    }

    public DynamoDBMapper getMapper() {
        return mapper;
    }
}
