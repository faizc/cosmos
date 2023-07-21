package com.sample.cosmos.diagnostics;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.InternalObjectNode;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedFlux;
import com.sample.cosmos.client.CosmosClientUtil;
import com.sample.cosmos.vo.Food;
import jdk.nashorn.internal.ir.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ClientDiagnosticsSamples {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientDiagnosticsSamples.class);
    public static CountDownLatch latch = new CountDownLatch(1);


    public static void main(String... args) throws Exception {
        CosmosAsyncClient client = CosmosClientUtil.getAsyncClientWithClientTelemetry();
        CosmosAsyncContainer container = CosmosClientUtil.getAsyncCollection(client
                , CosmosClientUtil.COLLECTION);
        //queryInPartitionWithId(container, "Baby Foods", "03246");
        queryInPartitionWithIdV2(container, "Baby Foods", "03246");
        latch.await();
    }


    public static void queryInPartitionWithIdV2(final CosmosAsyncContainer container,
                                                final String pkey,
                                                final String id) {
        Food food = new Food();
        food.setId("1");

        String successfulResponse = wrapWithSoftTimeoutAndFallback(
                container
                        .readItem(id,
                                new PartitionKey(pkey),
                                new CosmosItemRequestOptions(),
                                Food.class),
                Duration.ofNanos(10),
                food)
                .map(node -> node.getId())
                .block();
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
                .readItem(id, new PartitionKey(pkey), Food.class)
                .doOnSuccess(response -> {
                    // Log diagnostics if the duration exceeds 100ms
                    if(response.getDuration().compareTo(Duration.ofMillis(100))>0) {
                        LOGGER.info("Diagnostic String -- " + response.getDiagnostics());
                    }
                    // Log diagnostics if the request charge is greater than 10
                    if(response.getRequestCharge() > 10) {
                        LOGGER.info("Diagnostic String -- " + response.getDiagnostics());
                    }
                })
                .doOnError(exception -> {
                    if (exception instanceof CosmosException) {
                        LOGGER.info("Diagnostic String -- " + ((CosmosException) exception).getDiagnostics());
                    }
                })
                .subscribe();



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
                .doFinally(signalType -> {
                    System.out.println("Signal " + signalType);
                })
                .doOnError(error -> {
                    LOGGER.info("Message --> " + error.getMessage());
                    error.printStackTrace();
                    //latch.countDown();
                })
                .doOnComplete(() -> {
                    latch.countDown();
                })
                .subscribe();
    }

    static <Food> Mono<Food> wrapWithSoftTimeoutAndFallback(
            Mono<CosmosItemResponse<Food>> source,
            Duration softTimeout,
            Food fallback) {

        // Execute the readItem with transformation to return the json payload
        // asynchronously with a "soft timeout" - meaning when the "soft timeout"
        // elapses a default/fallback response is returned but the original async call is not
        // cancelled, but allowed to complete. This makes it possible to still emit diagnostics
        // or process the eventually successful call
        AtomicBoolean timeoutElapsed = new AtomicBoolean(false);
        return Mono
                .<Food>create(sink -> {
                    source
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    response -> {
                                        if (timeoutElapsed.get()) {
                                            LOGGER.warn(
                                                    "COMPLETED SUCCESSFULLY after timeout elapsed. Diagnostics: {}",
                                                    response.getDiagnostics().toString());
                                        } else {
                                            LOGGER.info("COMPLETED SUCCESSFULLY");
                                        }

                                        sink.success(response.getItem());
                                    },
                                    error -> {
                                        final Throwable unwrappedException = Exceptions.unwrap(error);
                                        if (unwrappedException instanceof CosmosException) {
                                            final CosmosException cosmosException = (CosmosException) unwrappedException;

                                            LOGGER.error(
                                                    "COMPLETED WITH COSMOS FAILURE. Diagnostics: {}",
                                                    cosmosException.getDiagnostics() != null ?
                                                            cosmosException.getDiagnostics().toString() : "n/a",
                                                    cosmosException);
                                        } else {
                                            LOGGER.error("COMPLETED WITH GENERIC FAILURE", error);
                                        }

                                        if (timeoutElapsed.get()) {
                                            // fallback returned already - don't emit unobserved error
                                            sink.success();
                                        } else {
                                            sink.error(error);
                                        }
                                    }
                            );
                })
                .timeout(softTimeout)
                .onErrorResume(error -> {
                    System.out.println("Timeout Elapsed");
                    timeoutElapsed.set(true);
                    return Mono.just(fallback);
                });
    }
}