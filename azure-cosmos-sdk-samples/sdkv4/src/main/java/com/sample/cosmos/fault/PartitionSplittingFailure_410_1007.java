package com.sample.cosmos.fault;

import com.azure.cosmos.*;
import com.azure.cosmos.models.FeedRange;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.test.faultinjection.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sample.cosmos.client.CosmosClientUtil;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PartitionSplittingFailure_410_1007 {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static String PARTITION_KEY_FIELD_NAME = "partitionKey";

    private static FaultInjectionServerErrorType FAULT_TYPE = FaultInjectionServerErrorType.PARTITION_IS_SPLITTING;

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
        List<FeedRange> feedRanges = container.getFeedRanges().block();
        System.out.println("Partition Size "+feedRanges.size());
        //
        FaultInjectionEndpoints faultInjectionEndpoint =
                new FaultInjectionEndpointBuilder(feedRanges.get(0))
                        .includePrimary(false)
                        .replicaCount(2)
                        .build();
        //
        FaultInjectionRule faultInjectionRule =
                new FaultInjectionRuleBuilder(FAULT_TYPE.name())
                        .condition(
                                new FaultInjectionConditionBuilder()
                                        .operationType(FaultInjectionOperationType.CREATE_ITEM)
                                        //.operationType(FaultInjectionOperationType.READ_ITEM)
                                        .endpoints(faultInjectionEndpoint)
                                        .region("West US")
                                        .connectionType(FaultInjectionConnectionType.DIRECT)
                                        .build()
                        )
                        .result(
                                FaultInjectionResultBuilders
                                        .getResultBuilder(FAULT_TYPE)
                                        .delay(Duration.ofMillis(1500))
                                        //.suppressServiceRequests(false)
                                        //.times(3)
                                        .build()
                        )
                        .duration(Duration.ofMinutes(2))
                        .build();
        CosmosFaultInjectionHelper.configureFaultInjectionRules(container, Arrays.asList(faultInjectionRule)).block();
        //
        CosmosDiagnostics cosmosDiagnostics = null;
        String ID = UUID.randomUUID().toString();
        try {
            cosmosDiagnostics = container.createItem(getDocumentDefinition(ID, "Mumbai"))
                    .block()
                    .getDiagnostics();
            if(cosmosDiagnostics != null) {
                System.out.println(" Cosmos Diagnostics - "+cosmosDiagnostics.toString());
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        /*ID = "c597f1e8-8314-4815-8239-b52212178d0b";
        try {
            cosmosDiagnostics = container.readItem(ID, new PartitionKey("Mumbai"), ObjectNode.class)
                    .block()
                    .getDiagnostics();
            if(cosmosDiagnostics != null) {
                System.out.println(" Read Cosmos Diagnostics - "+cosmosDiagnostics.toString());
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }*/
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
