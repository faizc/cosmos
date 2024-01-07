package com.sample.cosmos.request;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.implementation.guava25.collect.ImmutableList;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.sample.cosmos.client.CosmosClientUtil;
import com.sample.cosmos.vo.Food;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class E2ETimeout {

    private static final Logger LOGGER = LoggerFactory.getLogger(E2ETimeout.class);
    public static CountDownLatch latch = new CountDownLatch(1);


    public static void main(String... args) throws Exception {
        CosmosAsyncClient client = CosmosClientUtil.getAsyncClient();
        CosmosAsyncContainer container = CosmosClientUtil.getAsyncCollection(client
                , CosmosClientUtil.COLLECTION);
        queryInPartitionWithIdV2(container, "Baby Foods", "03246");
        latch.await();
    }

    public static void queryInPartitionWithIdV2(final CosmosAsyncContainer container,
                                                final String pkey,
                                                final String id) {
        Food food = new Food();
        food.setId("1");
        CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        options.setExcludedRegions(ImmutableList.of("East US"));

        container
                .readItem(id, new PartitionKey(pkey), options, Food.class)
                .doOnSuccess(foodCosmosItemResponse -> {
                    System.out.println("Item "+foodCosmosItemResponse.getRequestCharge() +
                            " Status "+foodCosmosItemResponse.getStatusCode());
                })
                .doOnError(exception -> {
                    exception.printStackTrace();
                })
                .subscribe();
    }

}
