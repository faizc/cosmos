package com.sample.cosmos.changefeed.fullfidelity;

import com.azure.cosmos.*;
import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.implementation.apachecommons.lang.RandomStringUtils;
import com.azure.cosmos.models.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.cosmos.client.CosmosClientUtil;
import com.sample.cosmos.vo.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class CFP_FF_Push {

    private static final ObjectMapper OBJECT_MAPPER = Utils.getSimpleObjectMapper();
    protected static Logger logger = LoggerFactory.getLogger(CFP_FF_Push.class);

    public static CountDownLatch latch = new CountDownLatch(2);

    private static ChangeFeedProcessor changeFeedProcessorInstance;
    private static boolean isWorkCompleted = false;

    private static ChangeFeedProcessorOptions options;
    private static List<Entity> documentList = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("BEGIN Sample");

        try {

            // <ChangeFeedProcessorOptions>
            options = new ChangeFeedProcessorOptions();
            options.setStartFromBeginning(false);
            //options.setLeasePrefix("myChangeFeedDeploymentUnit");
            // </ChangeFeedProcessorOptions>

            //Summary of the next four commands:
            //-Create an asynchronous Azure Cosmos DB client and database so that we can issue async requests to the DB
            //-Create a "feed container" and a "lease container" in the DB
            CosmosAsyncClient client = CosmosClientUtil.getAsyncClient();
            //
            CosmosAsyncDatabase cosmosDatabase = CosmosClientUtil.getAsyncDatabase(client);
            //
            CosmosAsyncContainer feedContainer = CosmosClientUtil.getAsyncCollection(client);
            // This needs to be done ONCE where you set the retention period.
            CosmosClientUtil.updateRetentionPeriod(client);
            //
            CosmosAsyncContainer leaseContainer = CosmosClientUtil.createNewLeaseCollection(client);
            // <StartChangeFeedProcessor>
            System.out.println("-->START Change Feed Processor on worker (handles changes asynchronously)");
            changeFeedProcessorInstance = getChangeFeedProcessorForAllVersionsAndDeletesMode("SampleHost_1",
                    feedContainer,
                    leaseContainer);
            changeFeedProcessorInstance.start()
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnSuccess(aVoid -> {
                        //pass
                    })
                    .subscribe();
            // </StartChangeFeedProcessor>

            //System.out.println("\n\nSTART application that inserts documents into feed container");
            //createNewDocumentsCustomPOJO(feedContainer, 10, Duration.ofSeconds(3));
            //upsertDocumentsCustomPOJO(feedContainer, 10, Duration.ofSeconds(3));
            //deleteDocumentsCustomPOJO(feedContainer, 10, Duration.ofSeconds(3));

            // Wait for some time
            latch.await();;
            // Close the CFP processor pipeline
            if (changeFeedProcessorInstance != null) {
                System.out.println("Stopped the change feed processor instance");
                changeFeedProcessorInstance.stop().subscribe();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("END Sample");
    }

    // <Delegate>
    public static ChangeFeedProcessor getChangeFeedProcessorForAllVersionsAndDeletesMode(String hostName,
                                                                                         CosmosAsyncContainer feedContainer,
                                                                                         CosmosAsyncContainer leaseContainer) {
        //
        System.out.println("Start ChangeFeedProcessor.....");

        return new ChangeFeedProcessorBuilder()
                .hostName(hostName)
                .options(options)
                .feedContainer(feedContainer)
                .leaseContainer(leaseContainer)
                /*.handleChanges((List<JsonNode> jsonnodes) -> {
                    for(JsonNode node : jsonnodes) {
                        System.out.println("json "+node);
                    }*/
                .handleAllVersionsAndDeletesChanges((List<ChangeFeedProcessorItem> changeFeedProcessorItems) -> {
                    System.out.println("--->handleAllVersionsAndDeletesChanges() START");

                    for (ChangeFeedProcessorItem item : changeFeedProcessorItems) {
                        try {
                            // AllVersionsAndDeletes Change Feed hands the document to you in the form of ChangeFeedProcessorItem
                            // As a developer you have two options for handling the ChangeFeedProcessorItem provided to you by Change Feed
                            // One option is to operate on the item as it is and call the different getters for different states, as shown below.
                            System.out.println("---->DOCUMENT RECEIVED: {}"+ OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(item));

                            System.out.println("---->CURRENT RECEIVED: {}"+ item.getCurrent());
                            System.out.println("---->PREVIOUS RECEIVED: {}"+ item.getPrevious());
                            System.out.println("---->METADATA RECEIVED: {}"+ item.getChangeFeedMetaData());

                            // You can also transform the ChangeFeedProcessorItem to JsonNode and work on the generic json structure.
                            // This is great especially if you do not have a single uniform data model for all documents.
                            System.out.println("----=>JsonNode received: " + item.toJsonNode());

                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("--->handleAllVersionsAndDeletesChanges() END");
                })
                .buildChangeFeedProcessor();
    }
    // </Delegate>




    public static void createNewDocumentsCustomPOJO(CosmosAsyncContainer containerClient, int count, Duration delay) {
        String suffix = RandomStringUtils.randomAlphabetic(10);
        for (int i = 0; i <= count; i++) {
            Entity document = new Entity();
            document.setId(String.format("0%d-%s", i, suffix));
            document.setPk(document.getId()); // This is a very simple example, so we'll just have a partition key (/pk) field that we set equal to id

            containerClient.createItem(document).subscribe(doc -> {
                System.out.println("---->DOCUMENT INSERT: " + doc);
                documentList.add(document);
            });

            long remainingWork = delay.toMillis();
            try {
                while (remainingWork > 0) {
                    Thread.sleep(100);
                    remainingWork -= 100;
                }
            } catch (InterruptedException iex) {
                // exception caught
                break;
            }
        }
    }

    public static void upsertDocumentsCustomPOJO(CosmosAsyncContainer containerClient, int count, Duration delay) {
        String suffix = RandomStringUtils.randomAlphabetic(10);
        for (int i = 0; i <= count; i++) {
            Entity document = new Entity();
            document.setId(String.format("0%d-%s", i, suffix));
            document.setPk(document.getId()); // This is a very simple example, so we'll just have a partition key (/pk) field that we set equal to id

            containerClient.upsertItem(document).subscribe(doc -> {
                System.out.println("---->DOCUMENT UPSERT: " + doc);
                documentList.add(document);
            });

            long remainingWork = delay.toMillis();
            try {
                while (remainingWork > 0) {
                    Thread.sleep(100);
                    remainingWork -= 100;
                }
            } catch (InterruptedException iex) {
                // exception caught
                break;
            }
        }
    }

    public static void deleteDocumentsCustomPOJO(CosmosAsyncContainer containerClient, int count, Duration delay) {
        for (int i = 0; i <= count; i++) {
            Entity document = documentList.get(i);
            containerClient.deleteItem(document.getId(), new PartitionKey(document.getPk())).subscribe(doc -> {
                System.out.println("---->DOCUMENT DELETE: " + doc);
            });

            long remainingWork = delay.toMillis();
            try {
                while (remainingWork > 0) {
                    Thread.sleep(100);
                    remainingWork -= 100;
                }
            } catch (InterruptedException iex) {
                // exception caught
                break;
            }
        }
    }

}
