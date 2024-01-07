package com.sample.cosmos.query.sync;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.implementation.apachecommons.lang.RandomStringUtils;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sample.cosmos.client.CosmosClientUtil;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class Writer {
    private static String PARTITION_KEY_FIELD_NAME = "lastName";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        CosmosAsyncClient client = CosmosClientUtil.getAsyncClient();
        CosmosAsyncContainer container = CosmosClientUtil.getAsyncCollection(client
                , CosmosClientUtil.COLLECTION);
        CountDownLatch latch = new CountDownLatch(2);
        //, CosmosClientUtil.COLLECTION_WITH_NO_INDEX);
        //latch.await();
        for (int i = 1; i <= Integer.MAX_VALUE; i++) {
            MyRunnable runnable = new MyRunnable(container);
            Thread thread = new Thread(runnable);
            thread.setName("Job"+i);
            //thread.start();

            String uuid = i + "";
            String partitionKey = "Sample" + RandomStringUtils.randomAlphabetic(10);
            //
            ObjectNode node = getDocumentDefinition(uuid, partitionKey);
            container
                    .upsertItem(node, new CosmosItemRequestOptions())
                    .doOnSuccess(response -> {
                        //   System.out.println(response.getStatusCode());
                    })
                    .doOnError(error -> {
                        //   System.out.println(error.getMessage());
                    })
                    .subscribe();
            System.out.println("ID " + uuid + " pkey " + partitionKey);

            if (i % 1000 == 0) {
                Thread.sleep(5 * 1000);
            }
            //System.out.println(response.getDiagnostics());
        }
        latch.await();;
    }

    static class MyRunnable implements Runnable {

        private CosmosAsyncContainer container;


        public MyRunnable(final CosmosAsyncContainer container) {
            this.container = container;
        }

        @Override
        public void run() {
            while (true) {
                String uuid = Thread.currentThread().getName() + RandomStringUtils.randomAlphabetic(10);
                String partitionKey = Thread.currentThread().getName() + RandomStringUtils.randomAlphabetic(10);
                //
                ObjectNode node = getDocumentDefinition(uuid, partitionKey);
                container
                        .upsertItem(node, new CosmosItemRequestOptions())
                        .publishOn(Schedulers.parallel())
                        .doOnSuccess(response -> {
                            System.out.println(response.getStatusCode());
                        })
                        .doOnError(error -> {
                            System.out.println(error.getMessage());
                        })
                        .subscribe();
                System.out.println("ID " + uuid + " pkey " + partitionKey);
            }
        }
    }

    private static ObjectNode getDocumentDefinition(String uuid, String partitionKey) {
        String json = String.format("{ "
                + "\"id\": \"%s\", "
                + "\"" + PARTITION_KEY_FIELD_NAME + "\": \"%s\", "
                + "\"prop\": \"%s\""
                + "}", uuid, partitionKey, UUID.randomUUID().toString());

        try {
            return OBJECT_MAPPER.readValue(json, ObjectNode.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Invalid partition key value provided.");
        }
    }
}
