package com.azure.cosmos.managementapi;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.Context;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.cosmos.fluent.models.*;
import com.azure.resourcemanager.cosmos.models.*;
import org.apache.commons.cli.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MongoThroughputSample {

    private static Config config = new Config();

    private static CommandLine setupOptions(String[] args) {
        /**
         * Usage:
         * --endpoint <cosmos account>
         * --database <database name>
         * --container <container name>
         * --resourcegroup <resourcegroup>
         */
        //
        Options commandLineOptions = new Options();
        //
        commandLineOptions.addOption(Option.builder().longOpt(Constants.ENDPOINT).required(true).hasArg().desc("Account name").build());
        commandLineOptions.addOption(Option.builder().longOpt(Constants.DATABASE).required(true).hasArg().desc("Database name").build());
        commandLineOptions.addOption(Option.builder().longOpt(Constants.CONTAINER).required(true).hasArg().desc("Container name").build());
        commandLineOptions.addOption(Option.builder().longOpt(Constants.RESOURCEGROUP).required(true).hasArg().desc("Resource Group name").build());
        //
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;
        //
        try {
            commandLine = parser.parse(commandLineOptions, args);
        } catch (ParseException | NumberFormatException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("Cosmos Management API ", commandLineOptions);
        }
        //
        return commandLine;
    }

    private static Config populateConfig(final CommandLine commandLine) {
        //
        config.setEndpoint(commandLine.getOptionValue(Constants.ENDPOINT));
        config.setContainer(commandLine.getOptionValue(Constants.CONTAINER));
        config.setDatabase(commandLine.getOptionValue(Constants.DATABASE));
        config.setResourceGroup(commandLine.getOptionValue(Constants.RESOURCEGROUP));
        return config;
    }

    public static void scaleCollection(final AzureResourceManager azureResourceManager) {
        //
        new MongoDBCollectionCreateUpdateParameters()
                .withLocation(Region.US_WEST.label());
        //
        ThroughputSettingsGetResultsInner containerResult = azureResourceManager.cosmosDBAccounts()
                //cosmosDBAccount
                .manager()
                .serviceClient()
                .getMongoDBResources()
                .updateMongoDBCollectionThroughput(config.getResourceGroup(),
                        config.getEndpoint(),
                        config.getDatabase(),
                        config.getContainer(),
                        new ThroughputSettingsUpdateParameters().withResource(
                                new ThroughputSettingsResource().withAutoscaleSettings(
                                        new AutoscaleSettingsResource().withMaxThroughput(1000)
                                )
                        ),
                        Context.NONE);
        System.out.println("Container Id " + containerResult.id());

        // Use the following code to validate the max throughput..
        ThroughputSettingsGetResultsInner throughputResult = azureResourceManager.cosmosDBAccounts()
                //cosmosDBAccount
                .manager()
                .serviceClient()
                .getMongoDBResources()
                .getMongoDBCollectionThroughput(config.getResourceGroup(),
                        config.getEndpoint(),
                        config.getDatabase(),
                        config.getContainer());
        System.out.println("Container Max Throughput " + throughputResult.resource().autoscaleSettings().maxThroughput());

    }

    public static void main(String... args) throws Exception {
        //
        CommandLine cline = setupOptions(args);
        if (cline != null) {
            populateConfig(cline);
            // Authenticate
            final AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
            final TokenCredential credential = new DefaultAzureCredentialBuilder()
                    .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint())
                    .build();
            //
            AzureResourceManager azureResourceManager = AzureResourceManager
                    .configure()
                    .withLogLevel(HttpLogDetailLevel.BASIC)
                    .authenticate(credential, profile)
                    .withSubscription("ff13286c-6387-46c7-a1b2-2dda443886ed")
                    ;
            // Print selected subscription
            System.out.println("Selected subscription: " + azureResourceManager.subscriptionId());
            // Create CosmosDB Global account
            scaleCollection(azureResourceManager);
            //
            System.out.println("\n\n <<< Exiting >>>  \n\n");
        }

    }

    @SuppressWarnings("unchecked")
    private static <T> Map<String, T> mapOf(Object... inputs) {
        Map<String, T> map = new HashMap<>();
        for (int i = 0; i < inputs.length; i += 2) {
            String key = (String) inputs[i];
            T value = (T) inputs[i + 1];
            map.put(key, value);
        }
        return map;
    }
}
