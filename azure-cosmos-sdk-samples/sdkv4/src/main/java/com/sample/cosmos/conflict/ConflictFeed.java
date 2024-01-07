package com.sample.cosmos.conflict;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncConflict;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosConflictProperties;
import com.azure.cosmos.models.CosmosConflictRequestOptions;
import com.azure.cosmos.models.CosmosConflictResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedFlux;
import com.sample.cosmos.client.CosmosClientUtil;

import java.util.Iterator;

public class ConflictFeed {
    public static void main(String[] args) {
        CosmosAsyncClient client = CosmosClientUtil.getAsyncClient();
        CosmosAsyncContainer container = CosmosClientUtil.getAsyncCollection(client
                , CosmosClientUtil.COLLECTION);

        int requestPageSize = 3;
        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();

        CosmosPagedFlux<CosmosConflictProperties> conflictReadFeedFlux = container.readAllConflicts(options);

        conflictReadFeedFlux.byPage(requestPageSize).toIterable().forEach(page -> {

            int expectedNumberOfConflicts = 0;
            int numberOfResults = 0;
            Iterator<CosmosConflictProperties> pageIt = page.getElements().iterator();

            while (pageIt.hasNext()) {
                CosmosConflictProperties conflictProperties = pageIt.next();

                // Read the conflict and committed item
                CosmosAsyncConflict conflict = container.getConflict(conflictProperties.getId());
                CosmosConflictResponse response = conflict.read(new CosmosConflictRequestOptions()).block();

                //
                System.out.println("conflictProperties id "+conflictProperties.getId()+ " response "+response);

            }
        });
    }
}
