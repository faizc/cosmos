package com.sample.cosmos.conflict;

import com.azure.cosmos.*;
import com.azure.cosmos.implementation.AsyncDocumentClient;
import com.azure.cosmos.implementation.DatabaseAccount;
import com.azure.cosmos.implementation.DatabaseAccountLocation;
import com.azure.cosmos.implementation.GlobalEndpointManager;
import com.azure.cosmos.models.*;
import com.sample.cosmos.client.CosmosClientUtil;
import com.sample.cosmos.vo.Food;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class CustomConflictResolution {
    private static List<CosmosAsyncClient> regionalClients = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        CosmosAsyncClient client = CosmosClientUtil.getAsyncClient();
        CosmosAsyncDatabase database = CosmosClientUtil.getAsyncDatabase(client);
        //
        AsyncDocumentClient asyncDocumentClient = BridgeInternal.getContextClient(client);
        GlobalEndpointManager globalEndpointManager = asyncDocumentClient.getGlobalEndpointManager();
        DatabaseAccount databaseAccount = globalEndpointManager.getLatestDatabaseAccount();
        Iterator<DatabaseAccountLocation> locationIterator = databaseAccount.getWritableLocations().iterator();
        while (locationIterator.hasNext()) {
            DatabaseAccountLocation accountLocation = locationIterator.next();
            List<String> prefferedLocations = new ArrayList<>();
            prefferedLocations.add(accountLocation.getName());
            System.out.println(accountLocation.getName());
            CosmosAsyncClient regionalClient = CosmosClientUtil.getAsyncClient(prefferedLocations);
            regionalClients.add(regionalClient);
        }
        // do not create the stored procedure so that conflicts get populated
        String sprocId = "conflictCustomSproc";
        String collectionId = "conflict"+UUID.randomUUID().toString().replace("-","");
        CosmosContainerProperties containerProperties = new CosmosContainerProperties(
                collectionId,
                "/foodGroup");
        ConflictResolutionPolicy resolutionPolicy = ConflictResolutionPolicy.createCustomPolicy(database.getId(),
                containerProperties.getId(),
                sprocId);
        containerProperties.setConflictResolutionPolicy(resolutionPolicy);
        database.createContainer(containerProperties, ThroughputProperties.createManualThroughput(400)).block();
        System.out.println("Wait for 30 seconds");
        Thread.sleep(30000); //waiting for container to get available across multi region

        /*
        ConflictResolutionPolicy resolutionPolicy = ConflictResolutionPolicy.createCustomPolicy(sprocId);
        // Create stored procedure
        String sprocBody = IOUtils.toString(
                CustomConflictResolution.class.getClassLoader().getResourceAsStream("conflict-resolver-sproc"),
                Charset.forName("UTF-8"));
        CosmosStoredProcedureProperties procedureProperties = new CosmosStoredProcedureProperties(sprocId,
                sprocBody);
        container.getScripts().createStoredProcedure(procedureProperties).block();
         */
        CosmosAsyncContainer container = CosmosClientUtil.getAsyncCollection(client, collectionId);
        //
        List<CosmosAsyncContainer> containers = new ArrayList<>();
        warmingUpClient(containers, database.getId(), container.getId());
        //Creating conflict by creating item in every region simultaneously
        String conflictId = "conflict";
        createItemsInParallelForConflicts(containers, conflictId);

        // Read all conflicts
        List<String> conflictIds = new ArrayList<>();
        Iterator<FeedResponse<CosmosConflictProperties>> iterator =
                containers.get(0).readAllConflicts(new CosmosQueryRequestOptions()).byPage().toIterable().iterator();
        List<Food> testPojos = new ArrayList<>();
        readConflicts(iterator, testPojos, conflictIds);
        System.out.println("conflictIds size "+conflictIds.size());
        // delete the container
        container.delete().block();
    }

    private static void readConflicts(Iterator<FeedResponse<CosmosConflictProperties>> iterator,
                               List<Food> pojoList,
                               List<String> conflictIds) {
        while (iterator.hasNext()) {
            for (CosmosConflictProperties conflict : iterator.next().getResults()) {
                pojoList.add(conflict.getItem(Food.class));
                if (conflictIds != null) {
                    conflictIds.add(conflict.getId());
                }
            }
        }
    }

    private static void warmingUpClient(List<CosmosAsyncContainer> asyncContainers, String dbId, String containerId)
            throws InterruptedException {
        System.out.println("regionalClients size "+regionalClients.size());
        for (CosmosAsyncClient asyncClient : regionalClients) {
            CosmosAsyncContainer container =
                    asyncClient.getDatabase(dbId).getContainer(containerId);
            Food warmUpItem = getFoodObject();
            for (int i = 1; i <= 4; i++) {
                try {
                    container.createItem(warmUpItem).block();
                    asyncContainers.add(container);
                    break;
                } catch (CosmosException ex) {
                    System.out.println(String.format(
                            "Container %s create has not reflected yet, retrying after 5 sec", containerId));
                    Thread.sleep(5000);//retry again after 5 sec
                }
            }
            container.readItem(warmUpItem.getId(), new PartitionKey(warmUpItem.getId()), null,
                    Food.class).block();
        }
    }

    private static Food getFoodObject() {
        Food test = new Food();
        String uuid = UUID.randomUUID().toString();
        test.setId(uuid);
        test.setFoodGroup(uuid);
        return test;
    }

    private static void createItemsInParallelForConflicts(List<CosmosAsyncContainer> containers, String conflictId) {
        System.out.println("containers sizes "+containers.size());
        for (int i = 0; i < containers.size(); i++) {
            int finalI = i;
            new Thread(() -> {
                Food conflictObject = new Food();
                conflictObject.setId(conflictId);
                conflictObject.setFoodGroup(conflictId);
                conflictObject.setDescription("Description of the food item "+ UUID.randomUUID().toString());
                tryInsertDocumentTest(containers.get(finalI), conflictObject).block();
            }).start();
        }
    }

    private static Mono<CosmosItemResponse<Food>> tryInsertDocumentTest(CosmosAsyncContainer container,
                                                                        Food test) {
        return container.createItem(test, new PartitionKey(test.getId()), new CosmosItemRequestOptions())
                .doOnSuccess(x->{
                    System.out.println(" Insert successful ");
                })
                .onErrorResume(e -> {
                    if (hasCosmosConflictException(e, 409)) {
                        return Mono.empty();
                    } else {
                        return Mono.error(e);
                    }
                });
    }

    private static boolean hasCosmosConflictException(Throwable e, int statusCode) {
        System.out.println("Status Code "+statusCode + " _ "+(e instanceof CosmosException));
        if (e instanceof CosmosException) {
            CosmosException cosmosException = (CosmosException) e;
            return cosmosException.getStatusCode() == statusCode;
        }
        return false;
    }
}
