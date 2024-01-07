package com.sample.cosmos.request;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sample.cosmos.client.CosmosClientUtil;

import java.io.*;

public class Reader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        CosmosClient cosmosClient = CosmosClientUtil.getClient();
        CosmosContainer cosmosContainer = CosmosClientUtil.getCollection(cosmosClient);
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader("C:/tmp/airroutes.json"));) {
            String line = bufferedReader.readLine();

            while (line != null) {
                System.out.println(line);
                cosmosContainer.upsertItem(OBJECT_MAPPER.readValue(line, ObjectNode.class));
                line = bufferedReader.readLine();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
