package com.sample.cosmos.changefeed.fullfidelity;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.models.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sample.cosmos.client.CosmosClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.*;

public class CFP_FF_Pull {
    public static CosmosAsyncClient clientAsync;
    private CosmosAsyncContainer container;
    private CosmosAsyncDatabase database;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static String PARTITION_KEY_FIELD_NAME = "partitionKey";

    protected static Logger logger = LoggerFactory.getLogger(CFP_FF_Pull.class);
    public final ArrayListValuedHashMap<String, ObjectNode> partitionKeyToDocuments = new ArrayListValuedHashMap<String, ObjectNode>();


    public static void main(String[] args) {
        CFP_FF_Pull p = new CFP_FF_Pull();

        try {
            System.out.println("Starting ASYNC main");
            p.changeFeedAllVersionsAndDeletesPullDemo();
            System.out.println("Demo complete, please hold while resources are released");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeFeedAllVersionsAndDeletesPullDemo() {

        clientAsync = CosmosClientUtil.getAsyncClient();
        this.container = CosmosClientUtil.getAsyncCollection(clientAsync);
        this.database = CosmosClientUtil.getAsyncDatabase(clientAsync);

        System.out.println("*************************************************************");
        System.out.println("*************************************************************");
        System.out.println("Reading change feed with all feed ranges on this machine...");
        System.out.println("*************************************************************");
        System.out.println("*************************************************************");

        // <FeedResponseIterator>
        CosmosChangeFeedRequestOptions options = CosmosChangeFeedRequestOptions
                .createForProcessingFromNow(FeedRange.forFullRange())
                .allVersionsAndDeletes();

        Iterator<FeedResponse<JsonNode>> responseIterator = container
                .queryChangeFeed(options, JsonNode.class)
                .byPage()
                .toIterable()
                .iterator();
        // </FeedResponseIterator>

        // <AllFeedRanges>
        int i = 0;
        List<JsonNode> results;
        while (responseIterator.hasNext()) {
            FeedResponse<JsonNode> response = responseIterator.next();
            results = response.getResults();
            System.out.println("Got " + results.size() + " items(s)");

            // applying the continuation token
            // only after processing all events
            options = CosmosChangeFeedRequestOptions
                    .createForProcessingFromContinuation(response.getContinuationToken())
                    .allVersionsAndDeletes();

            //  Insert, update and delete documents to get them in AllVersionsAndDeletes Change feed
            insertDocuments(2, 3);
            updateDocuments(2, 3);
            //deleteDocuments(2, 3);

            responseIterator = container
                    .queryChangeFeed(options, JsonNode.class)
                    .byPage()
                    .toIterable()
                    .iterator();
            //
            while (responseIterator.hasNext()) {
                response = responseIterator.next();
                results = response.getResults();
                System.out.println("Got " + results.size() + " items(s)");
            }
            i++;
            if (i > 2) {
                // artificially breaking out of loop - not required in a real app
                System.out.println("breaking....");
                break;
            }
        }
        // </AllFeedRanges>

        System.out.println("*************************************************************");
        System.out.println("*************************************************************");
        System.out.println("Finished Reading change feed using all feed ranges!");
        System.out.println("*************************************************************");
        System.out.println("*************************************************************");

        // <GetFeedRanges>
        Mono<List<FeedRange>> feedranges = container.getFeedRanges();
        List<FeedRange> feedRangeList = feedranges.block();
        System.out.println("feedRangeList count "+feedRangeList.size());
        // </GetFeedRanges>

        System.out.println("*************************************************************");
        System.out.println("*************************************************************");
        System.out.println("Simulate processing change feed on two separate machines");
        System.out.println("*************************************************************");
        System.out.println("*************************************************************");
        System.out.println("*************************************************************");
        System.out.println("*************************************************************");
        System.out.println("Start reading from machine 1....");
        System.out.println("*************************************************************");
        System.out.println("*************************************************************");

        // <Machine1>
        FeedRange range1 = feedRangeList.get(0);
        options = CosmosChangeFeedRequestOptions
                .createForProcessingFromNow(range1)
                .allVersionsAndDeletes();

        int machine1index = 0;
        responseIterator = container
                .queryChangeFeed(options, JsonNode.class)
                .byPage()
                .toIterable()
                .iterator();

        while (responseIterator.hasNext()) {
            FeedResponse<JsonNode> response = responseIterator.next();
            results = response.getResults();
            System.out.println("Got " + results.size() + " items(s) retrieved");

            // applying the continuation token
            // only after processing all events
            options = CosmosChangeFeedRequestOptions
                    .createForProcessingFromContinuation(response.getContinuationToken())
                    .allVersionsAndDeletes();

            //  Insert, update and delete documents to get them in AllVersionsAndDeletes Change feed
            insertDocuments(2, 3);
            updateDocuments(2, 3);
            deleteDocuments(2, 3);

            responseIterator = container
                    .queryChangeFeed(options, JsonNode.class)
                    .byPage()
                    .toIterable()
                    .iterator();

            machine1index++;

            if (machine1index > 2) {
                // artificially breaking out of loop - not required in a real app
                System.out.println("breaking....");
                break;
            }
        }
        // </Machine1>

        System.out.println("*************************************************************");
        System.out.println("*************************************************************");
        System.out.println("Finished reading feed ranges on machine 1!");
        System.out.println("*************************************************************");
        System.out.println("*************************************************************");

        System.out.println("*************************************************************");
        System.out.println("*************************************************************");

        System.out.println("*************************************************************");
        System.out.println("*************************************************************");
        System.out.println("Start reading from machine 2....");
        System.out.println("*************************************************************");
        System.out.println("*************************************************************");

        // <Machine2>
        FeedRange range2 = feedRangeList.get(1);
        options = CosmosChangeFeedRequestOptions
                .createForProcessingFromNow(range2)
                .allVersionsAndDeletes();

        responseIterator = container
                .queryChangeFeed(options, JsonNode.class)
                .byPage()
                .toIterable()
                .iterator();

        int machine2index = 0;

        while (responseIterator.hasNext()) {
            FeedResponse<JsonNode> response = responseIterator.next();
            results = response.getResults();
            System.out.println("Got " + results.size() + " items(s) retrieved");

            // applying the continuation token
            // only after processing all events
            options = CosmosChangeFeedRequestOptions
                    .createForProcessingFromContinuation(response.getContinuationToken())
                    .allVersionsAndDeletes();

            //  Insert, update and delete documents to get them in AllVersionsAndDeletes Change feed
            insertDocuments(2, 3);
            updateDocuments(2, 3);
            deleteDocuments(2, 3);

            responseIterator = container
                    .queryChangeFeed(options, JsonNode.class)
                    .byPage()
                    .toIterable()
                    .iterator();

            machine2index++;
            if (machine2index > 2) {
                // artificially breaking out of loop - not required in a real app
                System.out.println("breaking....");
                break;
            }
        }
        // </Machine2>

        System.out.println("*************************************************************");
        System.out.println("*************************************************************");
        System.out.println("Finished reading feed ranges on machine 2!");
        System.out.println("*************************************************************");
        System.out.println("*************************************************************");


        //grab first pk in keySet()
        Set<String> keySet = partitionKeyToDocuments.keySet();
        String partitionKey="";
        for (String string : keySet) {
            partitionKey = string;
            break;
        }

        System.out.println("*************************************************************");
        System.out.println("*************************************************************");
        System.out.println("Reading change feed from logical partition key!");
        System.out.println("*************************************************************");
        System.out.println("*************************************************************");

        // <PartitionKeyProcessing>
        options = CosmosChangeFeedRequestOptions
                .createForProcessingFromNow(FeedRange.forLogicalPartition(new PartitionKey(partitionKey)))
                .allVersionsAndDeletes();

        responseIterator = container
                .queryChangeFeed(options, JsonNode.class)
                .byPage()
                .toIterable()
                .iterator();

        int pkIndex = 0;

        while (responseIterator.hasNext()) {
            FeedResponse<JsonNode> response = responseIterator.next();
            results = response.getResults();
            System.out.println("Got " + results.size() + " items(s) retrieved");

            // applying the continuation token
            // only after processing all events
            options = CosmosChangeFeedRequestOptions
                    .createForProcessingFromContinuation(response.getContinuationToken())
                    .allVersionsAndDeletes();

            //  Insert, update and delete documents to get them in AllVersionsAndDeletes Change feed
            insertDocuments(2, 3);
            updateDocuments(2, 3);
            deleteDocuments(2, 3);

            responseIterator = container
                    .queryChangeFeed(options, JsonNode.class)
                    .byPage()
                    .toIterable()
                    .iterator();

            pkIndex++;
            if (pkIndex > 5) {
                // artificially breaking out of loop
                System.out.println("breaking....");
                break;
            }
        }
        // </PartitionKeyProcessing>

        System.out.println("*************************************************************");
        System.out.println("*************************************************************");
        System.out.println("Finished reading change feed from logical partition key!");
        System.out.println("*************************************************************");
        System.out.println("*************************************************************");

    }

    void insertDocuments(
            int partitionCount,
            int documentCount) {

        List<ObjectNode> docs = new ArrayList<>();

        for (int i = 0; i < partitionCount; i++) {
            String partitionKey = UUID.randomUUID().toString();
            for (int j = 0; j < documentCount; j++) {
                docs.add(getDocumentDefinition(partitionKey));
            }
        }

        ArrayList<Mono<CosmosItemResponse<ObjectNode>>> result = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            result.add(container
                    .createItem(docs.get(i)));
        }

        List<ObjectNode> insertedDocs = Flux.merge(
                        Flux.fromIterable(result),
                        10)
                .map(CosmosItemResponse::getItem).collectList().block();

        for (ObjectNode doc : insertedDocs) {
            partitionKeyToDocuments.put(
                    doc.get(PARTITION_KEY_FIELD_NAME).textValue(),
                    doc);
        }
        System.out.println("FINISHED INSERT");
    }

    void updateDocuments(
            int partitionCount,
            int documentCount) {

        Collection<ObjectNode> docs;
        for (int i = 0; i < partitionCount; i++) {
            String partitionKey = this.partitionKeyToDocuments
                    .keySet()
                    .stream()
                    .skip(i)
                    .findFirst()
                    .get();

            docs = partitionKeyToDocuments.get(partitionKey);

            for (int j = 0; j < documentCount; j++) {
                ObjectNode docToBeUpdated = docs.stream().skip(j).findFirst().get();
                docToBeUpdated.put("someProperty", UUID.randomUUID().toString());
                container.replaceItem(
                        docToBeUpdated,
                        docToBeUpdated.get("id").textValue(),
                        new PartitionKey(docToBeUpdated.get(PARTITION_KEY_FIELD_NAME).textValue()),
                        null).block();
            }
        }
        System.out.println("FINISHED UPSERT");
    }

    void deleteDocuments(
            int partitionCount,
            int documentCount) {

        Collection<ObjectNode> docs;
        for (int i = 0; i < partitionCount; i++) {
            String partitionKey = this.partitionKeyToDocuments
                    .keySet()
                    .stream()
                    .skip(i)
                    .findFirst()
                    .get();

            docs = this.partitionKeyToDocuments.get(partitionKey);

            for (int j = 0; j < documentCount; j++) {
                ObjectNode docToBeDeleted = docs.stream().findFirst().get();
                container.deleteItem(docToBeDeleted, null).block();
                docs.remove(docToBeDeleted);
            }
        }
        System.out.println("FINISHED DELETE");
    }

    private static ObjectNode getDocumentDefinition(String partitionKey) {
        String uuid = UUID.randomUUID().toString();
        String json = String.format("{ "
                + "\"id\": \"%s\", "
                + "\"" + PARTITION_KEY_FIELD_NAME + "\": \"%s\", "
                + "\"prop\": \"%s\""
                + "}", uuid, partitionKey, uuid);

        try {
            return OBJECT_MAPPER.readValue(json, ObjectNode.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Invalid partition key value provided.");
        }
    }


}
