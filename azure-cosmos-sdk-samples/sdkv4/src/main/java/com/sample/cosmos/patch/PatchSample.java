package com.sample.cosmos.patch;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosPatchItemRequestOptions;
import com.azure.cosmos.models.CosmosPatchOperations;
import com.azure.cosmos.models.PartitionKey;
import com.sample.cosmos.client.CosmosClientUtil;
import com.sample.cosmos.vo.Audience;
import com.sample.cosmos.vo.User;

import java.util.Arrays;

public class PatchSample {
    public static void main(String[] args) throws Exception {
        CosmosAsyncClient client = CosmosClientUtil.getAsyncClient();
        CosmosAsyncContainer container = CosmosClientUtil.getAsyncCollection(client
                , CosmosClientUtil.COLLECTION);
        //patchAdd(container, "123", "456", "789");
        //patchAddRemove(container, "999", "456"); //RequestCharge 34.78
        createItem(container);
        //Thread.sleep(15*1000);
        //patchAdd(container);
        //patchSet(container);
        //Thread.sleep(15*1000);
    }

    public static void patchAddRemove(CosmosAsyncContainer container, final String addElement, final String deleteElement) {
        //
        CosmosPatchOperations cosmosPatchOperations = CosmosPatchOperations.create();
        cosmosPatchOperations.remove("/audiences/" + deleteElement);
        cosmosPatchOperations.add("/audiences/"+addElement, "");
        //
        CosmosPatchItemRequestOptions options = new CosmosPatchItemRequestOptions();
        CosmosItemResponse<Object> response = container
                .patchItem("1", new PartitionKey(1), cosmosPatchOperations, options, Object.class)
                .block();
        System.out.println("Patch Status Code " + response.getStatusCode() +
                " RequestCharge " + response.getRequestCharge());
    }

    public static void patchRemove(CosmosAsyncContainer container, final String... keys) {
        //
        CosmosPatchOperations cosmosPatchOperations = CosmosPatchOperations.create();
        for (String key : keys)
            cosmosPatchOperations.remove("/audiences/" + key);
        //
        CosmosPatchItemRequestOptions options = new CosmosPatchItemRequestOptions();
        CosmosItemResponse<Object> response = container
                .patchItem("1", new PartitionKey(1), cosmosPatchOperations, options, Object.class)
                .block();
        System.out.println("Patch Status Code " + response.getStatusCode() +
                " RequestCharge " + response.getRequestCharge());
    }

    public static void patchSet(CosmosAsyncContainer container) {
        //
        CosmosPatchOperations cosmosPatchOperations = CosmosPatchOperations.create();
        Audience audience1 = new Audience(456, true);
//        cosmosPatchOperations.set("/audiences/0", Arrays.asList(audience1));
        cosmosPatchOperations.set("/audiences/0", audience1);
        //
        CosmosPatchItemRequestOptions options = new CosmosPatchItemRequestOptions();
        CosmosItemResponse<User> response = container
                .patchItem("1", new PartitionKey(1), cosmosPatchOperations, options, User.class)
                .block();
        System.out.println("Patch Status Code " + response.getStatusCode() +
                " RequestCharge " + response.getRequestCharge());
    }


    public static void patchAdd(CosmosAsyncContainer container, final String... keys) {
        //
        CosmosPatchOperations cosmosPatchOperations = CosmosPatchOperations.create();
        for(String key: keys)
            cosmosPatchOperations.add("/audiences/"+key, "");
        //
        CosmosPatchItemRequestOptions options = new CosmosPatchItemRequestOptions();
        CosmosItemResponse<Object> response = container
                .patchItem("1", new PartitionKey(1), cosmosPatchOperations, options, Object.class)
                .block();
        System.out.println("Patch Status Code " + response.getStatusCode() +
                " RequestCharge " + response.getRequestCharge());
    }

    public static void patchAdd(CosmosAsyncContainer container) {
        //
        CosmosPatchOperations cosmosPatchOperations = CosmosPatchOperations.create();
        Audience audience1 = new Audience(789, false);
//        cosmosPatchOperations.set("/audiences/0", Arrays.asList(audience1));
        cosmosPatchOperations.add("/audiences/0", audience1);
        //
        CosmosPatchItemRequestOptions options = new CosmosPatchItemRequestOptions();
        CosmosItemResponse<User> response = container
                .patchItem("1", new PartitionKey(1), cosmosPatchOperations, options, User.class)
                .block();
        System.out.println("Patch Status Code " + response.getStatusCode() +
                " RequestCharge " + response.getRequestCharge());
    }


    public static void patchReplace(CosmosAsyncContainer container) {
        //
        CosmosPatchOperations cosmosPatchOperations = CosmosPatchOperations.create();
        Audience audience1 = new Audience(456, true);
        cosmosPatchOperations.replace("/audiences", Arrays.asList(audience1));
        //
        CosmosPatchItemRequestOptions options = new CosmosPatchItemRequestOptions();
        CosmosItemResponse<User> response = container
                .patchItem("1", new PartitionKey(1), cosmosPatchOperations, options, User.class)
                .block();
        System.out.println("Patch Status Code " + response.getStatusCode() +
                " RequestCharge " + response.getRequestCharge());
    }

    public static void createItem(CosmosAsyncContainer container) {
        // Create a new item
        User user = new User();
        user.setId("4");
        user.setUserId(4);
        //user.setKeyValue(new KeyValue());
        user.getDetails().put("123", "");
        user.getDetails().put("456", "");
        //user.getAudiences().add(new Audience(123, false));
        //user.getAudiences().add(new Audience(456, false));
        // Pass in the object, and the SDK automatically extracts the full partition key path
        CosmosItemResponse<User> createResponse = container
                .createItem(user)
                .block();
        System.out.println("Status Code " + createResponse.getStatusCode() +
                " RequestCharge " + createResponse.getRequestCharge());

    }
}
