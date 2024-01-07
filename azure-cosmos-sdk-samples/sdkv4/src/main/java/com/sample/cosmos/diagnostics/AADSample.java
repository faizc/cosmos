package com.sample.cosmos.diagnostics;

import com.azure.core.credential.TokenRequestContext;
import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;

import java.time.Duration;

public class AADSample {

    public static void main(String... args) throws Exception {
        //
        String clientId = "dfdfd";
        String clientSecret = "ddfd";
        String tenantId = "dfdf";

        //
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();
        String accessToken = clientSecretCredential
                .getToken(new TokenRequestContext().addScopes("https://management.azure.com/.default")).block()
                .getToken();
        System.out.println("accessToken "+accessToken);
        //
        CosmosAsyncClient client = new CosmosClientBuilder()
                .endpoint("https://albaik.documents.azure.com:443/")
//                .endpoint("https://cassandraks.documents.azure.com:443/")
                .directMode(DirectConnectionConfig
                        .getDefaultConfig()
                        .setIdleConnectionTimeout(Duration.ofSeconds(30))
                        .setMaxConnectionsPerEndpoint(1000)
                        .setMaxRequestsPerConnection(100)
                )
                //.credential(clientSecretCredential)
                //.key("dsada==")
                .buildAsyncClient();

        CosmosAsyncDatabase db = client.getDatabase("Retailer");
        CosmosAsyncContainer container = db.getContainer("Family");

        for(int i=1; i<=10; i++) {
            CosmosItemResponse response = container
                    .readItem("36f0878f-3f83-4ad3-abfb-4d47169cc3fd", new PartitionKey("Seattle"), Object.class)
                    .block();
            System.out.println("RUs "+response.getDiagnostics());
        }
        System.out.println("End");

    }
}
