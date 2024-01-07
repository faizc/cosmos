package com.sample.cosmos.fault;

import com.azure.cosmos.*;
import com.azure.cosmos.test.faultinjection.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sample.cosmos.client.CosmosClientUtil;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

public class PartitionMigratingFailure_410_1008 {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static String PARTITION_KEY_FIELD_NAME = "partitionKey";

    public static void main(String[] args) {
        //
        CosmosEndToEndOperationLatencyPolicyConfigBuilder builder
                = new CosmosEndToEndOperationLatencyPolicyConfigBuilder(
//                Duration.ofMillis(1000*60*5));// 5 min.
                Duration.ofMillis(1000*60*1));
        //
        //
        DirectConnectionConfig directConnectionConfig = new DirectConnectionConfig();
        directConnectionConfig.setNetworkRequestTimeout(Duration.ofSeconds(2));
        directConnectionConfig.setConnectTimeout(Duration.ofSeconds(2));
        //
//        CosmosAsyncClient client = CosmosClientUtil.getAsyncClientWithE2EThresold(builder, directConnectionConfig);
        CosmosAsyncClient client = CosmosClientUtil.getAsyncClient();
        CosmosAsyncContainer container = CosmosClientUtil.getAsyncCollection(client);
        //
        FaultInjectionRule faultInjectionRule =
                new FaultInjectionRuleBuilder("PARTITION_IS_MIGRATING")
                        .condition(
                                new FaultInjectionConditionBuilder()
                                        .operationType(FaultInjectionOperationType.CREATE_ITEM)
                                        .build()
                        )
                        .result(
                                FaultInjectionResultBuilders
                                        .getResultBuilder(FaultInjectionServerErrorType.PARTITION_IS_MIGRATING)
                                        //.delay(Duration.ofMillis(20000))
                                        //.suppressServiceRequests(false)
                                        .times(3)
                                        .build()
                        )
                        .duration(Duration.ofMinutes(5))
                        .build();
        CosmosFaultInjectionHelper.configureFaultInjectionRules(container, Arrays.asList(faultInjectionRule)).block();
        //
        CosmosDiagnostics cosmosDiagnostics = null;
        try {
            cosmosDiagnostics = container.createItem(getDocumentDefinition(UUID.randomUUID().toString(), "Mumbai"))
                    .block()
                    .getDiagnostics();
            if(cosmosDiagnostics != null) {
                System.out.println(" Cosmos Diagnostics - "+cosmosDiagnostics.toString());
            }
        } catch (Exception exception) {
            exception.printStackTrace();
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
