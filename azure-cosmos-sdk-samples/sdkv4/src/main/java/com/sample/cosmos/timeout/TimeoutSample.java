package com.sample.cosmos.timeout;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.sample.cosmos.client.CosmosClientUtil;
import com.sample.cosmos.vo.Food;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TimeoutSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutSample.class);
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