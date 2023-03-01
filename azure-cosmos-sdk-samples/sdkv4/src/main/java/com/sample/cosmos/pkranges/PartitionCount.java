package com.sample.cosmos.pkranges;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosBridgeInternal;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.implementation.PartitionKeyRange;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import reactor.core.publisher.Flux;

public class PartitionCount {

    public static void main(String... args) {

        CosmosAsyncClient client = new CosmosClientBuilder()
                .endpoint("https://velocity.documents.azure.com:443/")
                //.key("dsadasdas")
                .buildAsyncClient();

        com.azure.cosmos.implementation.AsyncDocumentClient internalClient =
                CosmosBridgeInternal.getAsyncDocumentClient(client);

        Flux<FeedResponse<PartitionKeyRange>> feedObservable = internalClient.readPartitionKeyRanges(
                getCollectionLink("Retailer", "Food"),
                new CosmosQueryRequestOptions());

        for (PartitionKeyRange result : feedObservable.blockFirst().getResults()) {
            System.out.println("result "+result);
        }


    }


    private static String getCollectionLink(final String database,
                                            final String collection) {
        return "dbs/" + database + "/colls/" + collection;
    }
}
