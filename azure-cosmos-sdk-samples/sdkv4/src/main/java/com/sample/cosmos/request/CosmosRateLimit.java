package com.sample.cosmos.request;

import com.azure.cosmos.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sample.cosmos.client.CosmosClientUtil;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CosmosRateLimit {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static String PARTITION_KEY_FIELD_NAME = "lastName";

    public static void main(String[] args) throws Exception {
        CosmosAsyncClient cosmosClient = CosmosClientUtil.getAsyncClient();
        CosmosAsyncContainer cosmosContainer = CosmosClientUtil.getAsyncCollection(cosmosClient);

        for(int i=1; i<10;i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int count = 0;
                    while (true) {
                        String uuid = UUID.randomUUID().toString();
                        System.out.println(Thread.currentThread().getName()+" Writing data "+uuid);
                        ObjectNode node = getDocumentDefinition(uuid, uuid);
                        try {
//                            int code = cosmosContainer.upsertItem(node).block().getStatusCode();
//                            System.out.println("code "+code);
                            cosmosContainer
                                    .upsertItem(node)
                                    .doOnSuccess(objectNodeCosmosItemResponse -> System.out.println("Status "+ objectNodeCosmosItemResponse.getStatusCode()))
                                    .subscribe();
                            count++;
                            if(count==1000) {
                                Thread.sleep(1000*10*2);
                                count=0;
                            }
                        }catch (CosmosException ce) {
                            ce.printStackTrace();;
                        }catch (Exception ce) {
                            ce.printStackTrace();;
                        }
                    }
                }
            });
            thread.setName("Thread-"+i);
            thread.start();
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
