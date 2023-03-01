package com.sample.cosmos.client;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;

import java.time.Duration;

public class CosmosClientUtil {

    public static final String ENDPOINT = "https://albaik.documents.azure.com:443/";
    public static final String KEY = "==";
    public static final String DATABASE = "ChangeFeedDemo";
    public static final String COLLECTION = "Entity";
    public static final String LEASE_COLLECTION = "Entity-Lease";
    public static final String COLLECTION_WITH_DEFAULT_INDEX = "FoodDefaultIdx";
    public static final String COLLECTION_WITH_NO_INDEX = "FoodNoIdx";
    public static final String COLLECTION_WITH_CUSTOM_INDEX = "CustomIndex";

    public static CosmosClient getClient() {
        CosmosClient client = new CosmosClientBuilder()
                .endpoint(ENDPOINT)
                .key(KEY)
                .contentResponseOnWriteEnabled(true)
                .buildClient();
        return client;
    }

    public static CosmosDatabase getDatabase(final CosmosClient client) {
        return client.getDatabase(DATABASE);
    }

    public static CosmosContainer getCollection(final CosmosClient client) {
        return client.getDatabase(DATABASE).getContainer(COLLECTION);
    }

    public static CosmosContainer getCollection(final CosmosClient client, final String colName) {
        System.out.println("Database "+DATABASE+ " Collection "+colName);
        return client.getDatabase(DATABASE).getContainer(colName);
    }

    public static CosmosAsyncClient getAsyncClient() {
        CosmosAsyncClient client = new CosmosClientBuilder()
                .endpoint(ENDPOINT)
                .key(KEY)
                .contentResponseOnWriteEnabled(true)
                .buildAsyncClient();
        return client;
    }

    public static CosmosAsyncDatabase getAsyncDatabase(final CosmosAsyncClient client) {
        return client.getDatabase(DATABASE);
    }

    public static CosmosAsyncContainer getAsyncCollection(final CosmosAsyncClient client) {
        return client.getDatabase(DATABASE).getContainer(COLLECTION);
    }

    public static CosmosAsyncContainer getAsyncCollection(final CosmosAsyncClient client, final String colName) {
        System.out.println("Database "+DATABASE+ " Collection "+colName);
        return client.getDatabase(DATABASE).getContainer(colName);
    }

    // Update the amount of time you would want the data to be retained
    public static void updateRetentionPeriod(final CosmosAsyncClient client) {
        CosmosAsyncDatabase databaseLink = client.getDatabase(DATABASE);
        CosmosAsyncContainer collectionLink = databaseLink.getContainer(COLLECTION);
        //
        if (collectionLink != null) {
            //
            CosmosContainerProperties containerSettings = collectionLink.read().block().getProperties();
            containerSettings.setChangeFeedPolicy(ChangeFeedPolicy.createAllVersionsAndDeletesPolicy(Duration.ofMinutes(15)));
            CosmosContainerResponse containerResponse = collectionLink.replace(containerSettings).block();
            System.out.println("Retention Period "+containerResponse.getProperties().getChangeFeedPolicy().getRetentionDurationForAllVersionsAndDeletesPolicy());
        }
    }

    public static CosmosAsyncContainer createNewLeaseCollection(CosmosAsyncClient client) {
        CosmosAsyncDatabase databaseLink = client.getDatabase(DATABASE);
        CosmosAsyncContainer leaseCollectionLink = databaseLink.getContainer(LEASE_COLLECTION);
        CosmosContainerResponse leaseContainerResponse = null;

        try {
            leaseContainerResponse = leaseCollectionLink.read().block();
            /*
            if (leaseContainerResponse != null) {
                leaseCollectionLink.delete();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }*/
            return databaseLink.getContainer(leaseContainerResponse.getProperties().getId());
        } catch (RuntimeException ex) {
            if (ex instanceof CosmosException) {
                CosmosException CosmosException = (CosmosException) ex;

                if (CosmosException.getStatusCode() != 404) {
                    throw ex;
                }
            } else {
                throw ex;
            }
        }
        //
        CosmosContainerProperties containerSettings = new CosmosContainerProperties(LEASE_COLLECTION, "/id");
        CosmosContainerRequestOptions requestOptions = new CosmosContainerRequestOptions();
        //
        ThroughputProperties throughputProperties = ThroughputProperties.createManualThroughput(400);
        //
        leaseContainerResponse = databaseLink.createContainer(containerSettings, throughputProperties, requestOptions).block();
        //
        if (leaseContainerResponse == null) {
            throw new RuntimeException(String.format("Failed to create collection %s in database %s.", LEASE_COLLECTION, DATABASE));
        }
        //
        return databaseLink.getContainer(leaseContainerResponse.getProperties().getId());
    }

}