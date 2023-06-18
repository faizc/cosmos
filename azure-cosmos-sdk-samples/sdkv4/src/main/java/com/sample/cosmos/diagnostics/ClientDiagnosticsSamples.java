package com.sample.cosmos.diagnostics;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedFlux;
import com.sample.cosmos.client.CosmosClientUtil;
import com.sample.cosmos.vo.Food;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class ClientDiagnosticsSamples {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientDiagnosticsSamples.class);
    public static CountDownLatch latch = new CountDownLatch(1);


    public static void main(String... args) throws Exception {
        CosmosAsyncClient client = CosmosClientUtil.getAsyncClientWithClientTelemetry();
        CosmosAsyncContainer container = CosmosClientUtil.getAsyncCollection(client
                , CosmosClientUtil.COLLECTION);
        queryInPartitionWithId(container, "Baby Foods", "03246");
        latch.await();
    }

    public static void queryInPartitionWithId(final CosmosAsyncContainer container,
                                              final String pkey,
                                              final String id) {
        // Set some common query options
        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        //queryOptions.setEnableCrossPartitionQuery(true); //No longer necessary in SDK v4
        //  Set query metrics enabled to get metrics around query executions
        queryOptions.setQueryMetricsEnabled(true);

        container
                .queryItems(
                        "SELECT * FROM Food WHERE Food.id = '" + id + "' and Food.foodGroup = '" + pkey + "' ", queryOptions, Food.class)
                .byPage(10).map(foodFeedResponse -> {
                    LOGGER.info("Got a page of query result with {} items(s), request charge of {}, activity-id {}",
                            foodFeedResponse.getResults().size(),
                            foodFeedResponse.getRequestCharge(),
                            foodFeedResponse.getActivityId());
                    foodFeedResponse.getContinuationToken();
                    LOGGER.info("Item Ids {}", foodFeedResponse
                            .getResults()
                            .stream()
                            .map(Food::getId)
                            .collect(Collectors.toList()));
                    return Flux.empty();
                })
                .timeout(Duration.ofMillis(1000L))
                .doOnError(error -> {
                    LOGGER.info("Message --> "+error.getMessage());
                    error.printStackTrace();
                    latch.countDown();
                })
                .doOnComplete(() -> {
                    latch.countDown();
                })
                .subscribe();
    }
}
