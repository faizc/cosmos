package com.sample.cosmos.patch;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosPatchItemRequestOptions;
import com.azure.cosmos.models.CosmosPatchOperations;
import com.azure.cosmos.models.PartitionKey;
import com.sample.cosmos.client.CosmosClientUtil;
import com.sample.cosmos.vo.UserAudienceInfo;

import java.util.concurrent.CountDownLatch;
/*
Take documents of different sizes
evaluate the point read operation time across different documents

 */
public class PatchVsReplace {
    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CosmosAsyncClient client = CosmosClientUtil.getAsyncClient();
        CosmosAsyncContainer container = CosmosClientUtil.getAsyncCollection(client
                , CosmosClientUtil.COLLECTION);
        createItem(container);
        patchItem(container, 9, 456);
        readReplaceItem(container, 1, 789);
        deleteItem(container);
        latch.await();
    }

    public static void readReplaceItem(final CosmosAsyncContainer container,
                                       final Integer newElement,
                                       final Integer deleteElement) {
        //
        CosmosItemResponse<UserAudienceInfo> createResponse = container
                .readItem("4", new PartitionKey(4), UserAudienceInfo.class)
                .block();
        System.out.println("ReadItem Status Code " + createResponse.getStatusCode() +
                " RequestCharge " + createResponse.getRequestCharge());
        UserAudienceInfo user = createResponse.getItem();
        //
        for(int i=0;i<newElement;i++) {
            int index = i+2;
            int value = 200 + i;
            user.getAudiences().add(value);
        }
        user.getAudiences().remove(deleteElement);
        //CosmosItemResponse<UserAudienceInfo> response =
                container
                .replaceItem(user, "4", new PartitionKey(4))
                .doOnSuccess(res -> {
                    System.out.println("Replace StatusCode "+res.getStatusCode()+ " Charge "+res.getRequestCharge()+
                            " Diagnostics "+res.getDiagnostics());
                })
                .subscribe();
     //           .block();
    }

    public static void patchItem(final CosmosAsyncContainer container,
                                 final Integer newElement,
                                 final Integer deleteElement) {
        //
        CosmosPatchOperations cosmosPatchOperations = CosmosPatchOperations.create();
        //The number of patch operations cannot exceed '10'.
        for(int i=0;i<newElement;i++) {
            int index = i+2;
            int value = 100 + i;
            cosmosPatchOperations.add("/audiences/" + index, value);
        }
        cosmosPatchOperations.remove("/audiences/0");
        //
        CosmosPatchItemRequestOptions options = new CosmosPatchItemRequestOptions();
        CosmosItemResponse<UserAudienceInfo> response = container
                .patchItem("4", new PartitionKey(4), cosmosPatchOperations, options, UserAudienceInfo.class)
                .block();
        System.out.println("Patch Status Code " + response.getStatusCode() +
                " RequestCharge " + response.getRequestCharge()+
                " Diagnostics "+response.getDiagnostics().getDiagnosticsContext());
    }

    public static void createItem(CosmosAsyncContainer container) {
        // Create a new item
        UserAudienceInfo user = new UserAudienceInfo();
        user.setId("4");
        user.setUserId(4);
        user.getAudiences().add(123);
        user.getAudiences().add(456);
        for (int count=1; count <=10000; count++) {
            user.getAudiences().add(10*count);
        }
        // Pass in the object, and the SDK automatically extracts the full partition key path
        CosmosItemResponse<UserAudienceInfo> createResponse = container
                .createItem(user)
                .block();
        System.out.println("CreateItem Status Code " + createResponse.getStatusCode() +
                " RequestCharge " + createResponse.getRequestCharge()+
                " Diagnostics "+createResponse.getDiagnostics());
    }

    public static void deleteItem(CosmosAsyncContainer container) {
        // Create a new item
        // Pass in the object, and the SDK automatically extracts the full partition key path
        CosmosItemResponse<Object> createResponse = container
                .deleteItem("4", new PartitionKey(4))
                .block();
        System.out.println("DeleteItem Status Code " + createResponse.getStatusCode() +
                " RequestCharge " + createResponse.getRequestCharge()+
                " Diagnostics "+createResponse.getDiagnostics());
    }
}
