package org.unizin.cmp.harvester.job;

import java.util.List;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.ConsistentReads;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;


public final class DynamoDBTestClient {
    public static final String DEFAULT_TEST_TABLE = "TestTable";

    final String tableName;
    final AmazonDynamoDB dynamoDB;
    final DynamoDBMapper mapper;


    public DynamoDBTestClient() {
        this(DEFAULT_TEST_TABLE);
    }

    public DynamoDBTestClient(final String tableName) {
        this.tableName = tableName;
        dynamoDB = new AmazonDynamoDBClient(new BasicAWSCredentials("", ""));
        dynamoDB.setEndpoint(String.format("http://127.0.0.1:%s",
                Tests.DYNAMO_PORT));
        final DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder()
                .withTableNameOverride(new TableNameOverride(tableName))
                .withConsistentReads(ConsistentReads.CONSISTENT)
                .build();
        mapper = new DynamoDBMapper(dynamoDB, config);
    }

    public void dropTable() {
        dynamoDB.deleteTable(tableName);
    }

    public void createTable(final Class<?> mappedClass) {
        // Throughput: ignored by local, but still required.
        final ProvisionedThroughput throughput = new ProvisionedThroughput(1L,
                1L);
        final CreateTableRequest req = mapper.generateCreateTableRequest(
                mappedClass)
                .withProvisionedThroughput(throughput)
                .withTableName(tableName);
        dynamoDB.createTable(req);
    }

    public int countItems(final Class<?> mappedClass) {
        final DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        return countItems(mappedClass, scanExpression);
    }

    public int countItems(final Class<?> mappedClass,
            final DynamoDBScanExpression scanExpression) {
        return mapper.count(mappedClass, scanExpression);
    }

    public <T> List<T> scan(final Class<T> mappedClass) {
        return mapper.scan(mappedClass, new DynamoDBScanExpression());
    }
}
