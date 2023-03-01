package com.sample.cosmos.changefeed;

import com.azure.cosmos.*;
import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.models.ChangeFeedProcessorOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sample.cosmos.client.CosmosClientUtil;
import com.sample.cosmos.vo.Entity;
import reactor.core.scheduler.Schedulers;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class CFPPush {

    public static CountDownLatch latch = new CountDownLatch(2);
    private static final ObjectMapper OBJECT_MAPPER = Utils.getSimpleObjectMapper();

    public static void main(String[] args) throws Exception {
        //Summary of the next four commands:
        //-Create an asynchronous Azure Cosmos DB client and database so that we can issue async requests to the DB
        //-Create a "feed container" and a "lease container" in the DB
        CosmosAsyncClient client = CosmosClientUtil.getAsyncClient();
        //
        CosmosAsyncDatabase cosmosDatabase = CosmosClientUtil.getAsyncDatabase(client);
        //
        CosmosAsyncContainer feedContainer = CosmosClientUtil.getAsyncCollection(client);
        //
        CosmosAsyncContainer leaseContainer = CosmosClientUtil.createNewLeaseCollection(client);

        // <StartChangeFeedProcessor>

        System.out.println("-->START Change Feed Processor on worker (handles changes asynchronously)");
        ChangeFeedProcessor changeFeedProcessorInstance = getChangeFeedProcessor("SampleHost_2", feedContainer, leaseContainer);
        changeFeedProcessorInstance.start()
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(aVoid -> {
                    //pass

                })
                .subscribe();
        // </StartChangeFeedProcessor>
        // Wait for some time
        latch.await();;
        // Close the CFP processor pipeline
        if (changeFeedProcessorInstance != null) {
            System.out.println("Stopped the change feed processor instance");
            changeFeedProcessorInstance.stop().subscribe();
        }

    }

    // <Delegate>
    public static ChangeFeedProcessor getChangeFeedProcessor(String hostName, CosmosAsyncContainer feedContainer, CosmosAsyncContainer leaseContainer) {
        //
        ChangeFeedProcessorOptions options = new ChangeFeedProcessorOptions();
        options.setStartFromBeginning(true);
        //options.setLeasePrefix("myChangeFeedDeploymentUnit");

        return new ChangeFeedProcessorBuilder()
                .hostName(hostName)
                .options(options)
                .feedContainer(feedContainer)
                .leaseContainer(leaseContainer)
                .handleChanges((List<JsonNode> docs) -> {
                    System.out.println("--->setHandleChanges() START");

                    for (JsonNode document : docs) {
                        try {
                            //Change Feed hands the document to you in the form of a JsonNode
                            //As a developer you have two options for handling the JsonNode document provided to you by Change Feed
                            //One option is to operate on the document in the form of a JsonNode, as shown below. This is great
                            //especially if you do not have a single uniform data model for all documents.
                            System.out.println("---->DOCUMENT RECEIVED: " + OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(document));

                            //You can also transform the JsonNode to a POJO having the same structure as the JsonNode,
                            //as shown below. Then you can operate on the POJO.
                            Entity pojo_doc = OBJECT_MAPPER.treeToValue(document, Entity.class);
                            System.out.println("----=>id: " + pojo_doc.getId());

                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("--->handleChanges() END");

                })
                .buildChangeFeedProcessor();
    }
    // </Delegate>

}
