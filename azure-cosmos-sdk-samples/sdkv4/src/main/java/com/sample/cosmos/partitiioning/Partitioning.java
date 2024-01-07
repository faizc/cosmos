package com.sample.cosmos.partitiioning;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.sample.cosmos.client.CosmosClientUtil;
import com.sample.cosmos.vo.Audience;
import com.sample.cosmos.vo.User;

public class Partitioning {
    public static void main(String[] args) throws Exception {
        CosmosAsyncClient client = CosmosClientUtil.getAsyncClient();
        CosmosAsyncContainer container = CosmosClientUtil.getAsyncCollection(client
                , CosmosClientUtil.COLLECTION);
        createItem(container);
        Thread.sleep(20*1000);
        updateItem(container);
    }

    public static void updateItem(CosmosAsyncContainer container) {
        // Create a new item
        User user = new User();
        user.setId("1");
        user.setUserId(1);
        /*user.getAudiences().add(new Audience(999, false));
        user.getAudiences().add(new Audience(789, true));
        //
        user.getAudiencesString().add("123");
        user.getAudiencesString().add("456");
        user.getAudiencesString().add("456");
        user.getAudiencesString().add("789");*/
        // Pass in the object, and the SDK automatically extracts the full partition key path
        CosmosItemResponse<User> createResponse = container
                .upsertItem(user)
                .block();
        System.out.println("Status Code " + createResponse.getStatusCode() +
                " RequestCharge " + createResponse.getRequestCharge());

    }

    public static void createItem(CosmosAsyncContainer container) {
        // Create a new item
        User user = new User();
        user.setId("1");
        user.setUserId(1);
        /*user.getAudiences().add(new Audience(123, false));
        user.getAudiences().add(new Audience(456, false));
        user.getAudiences().add(new Audience(789, false));
        //user.getAudiences().add(new Audience(789, true));
        //
        user.getAudiencesString().add("123");
        user.getAudiencesString().add("456");
        user.getAudiencesString().add("456");
        user.getAudiencesString().add("789");*/
        // Pass in the object, and the SDK automatically extracts the full partition key path
        CosmosItemResponse<User> createResponse = container
                .createItem(user)
                .block();
        System.out.println("Status Code " + createResponse.getStatusCode() +
                " RequestCharge " + createResponse.getRequestCharge());

    }


}
