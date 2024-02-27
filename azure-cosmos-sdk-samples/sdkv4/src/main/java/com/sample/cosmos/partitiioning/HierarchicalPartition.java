package com.sample.cosmos.partitiioning;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.PartitionKeyBuilder;
import com.azure.cosmos.util.CosmosPagedFlux;
import com.sample.cosmos.client.CosmosClientUtil;
import com.sample.cosmos.vo.UserAudience;
import reactor.core.publisher.Flux;

public class HierarchicalPartition {
    public static void main(String[] args) {
        CosmosAsyncClient client = CosmosClientUtil.getAsyncClient();
        CosmosAsyncContainer container = CosmosClientUtil.getAsyncCollection(client
                , CosmosClientUtil.COLLECTION);
        //createItem(container);
        //query(container);
        pointRead(container);
    }

    public static void createItem(CosmosAsyncContainer container) {
        System.out.println("Adding 10k records ");
        for (int i = 1; i <= 10000; i++) {
            // Create a new item
            UserAudience item = new UserAudience();
            item.setId(1 + "");
            item.setUserId(1);
            item.setAudience(i);
            // Pass in the object, and the SDK automatically extracts the full partition key path
            CosmosItemResponse<UserAudience> createResponse = container
                    .createItem(item)
                    .block();

        }
        System.out.println("Finished ");
    }

    public static void pointRead(CosmosAsyncContainer container) {
        // Store the unique identifier
        String id = "1";

        // Build the full partition key path
        PartitionKey partitionKey = new PartitionKeyBuilder()
                .add(1) //UserId
                .add("856b39d1-a114-49cc-ad80-288ebd3d5cbe") //SessionId
                .build();

        // Perform a point read
        CosmosItemResponse<UserAudience> readResponse = container
                .readItem(id, partitionKey, UserAudience.class)
                .block();
        System.out.println("ReadResponse " + readResponse.getDiagnostics().toString());
    }

    public static void query(CosmosAsyncContainer container) {
        // Define a single-partition query that specifies the full partition key path
        String query = String.format(
                "SELECT * FROM c WHERE c.id = '%s' AND c.userId = %s AND c.audience = %s",
                "1",
                "1",
                "9000"
        );

        // Retrieve an iterator for the result set
        CosmosPagedFlux<UserAudience> pagedResponse = container.queryItems(
                query, new CosmosQueryRequestOptions(), UserAudience.class);

        pagedResponse
                .byPage()
                .flatMap(fluxResponse -> {
                    System.out.println(fluxResponse.getCosmosDiagnostics().toString());

                    for (UserAudience result : fluxResponse.getResults()) {
                    }
                    return Flux.empty();
                }).blockLast();
    }

}
