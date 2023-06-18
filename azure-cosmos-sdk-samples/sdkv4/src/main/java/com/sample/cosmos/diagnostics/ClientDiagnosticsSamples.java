package com.sample.cosmos.diagnostics;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.sample.cosmos.client.CosmosClientUtil;

public class ClientDiagnosticsSamples {
    public static void main(String... args) {
        CosmosAsyncClient client = CosmosClientUtil.getAsyncClientWithClientTelemetry();
        CosmosAsyncContainer container = CosmosClientUtil.getAsyncCollection(client
                , CosmosClientUtil.COLLECTION_WITH_DEFAULT_INDEX);
    }
}
