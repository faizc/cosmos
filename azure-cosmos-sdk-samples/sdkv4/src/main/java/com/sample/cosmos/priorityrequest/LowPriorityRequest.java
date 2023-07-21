package com.sample.cosmos.priorityrequest;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.ThroughputControlGroupConfig;
import com.azure.cosmos.ThroughputControlGroupConfigBuilder;
import com.azure.cosmos.models.*;
import com.sample.cosmos.client.CosmosClientUtil;
import com.sample.cosmos.query.sync.QueryPatterns;
import com.sample.cosmos.vo.Food;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;

public class LowPriorityRequest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LowPriorityRequest.class);

    public static void main(String... args) {
        CosmosClient client = CosmosClientUtil.getClient();
        CosmosContainer container = CosmosClientUtil.getCollection(client
                , CosmosClientUtil.COLLECTION);

        ThroughputControlGroupConfig groupConfig =
                new ThroughputControlGroupConfigBuilder()
                        .groupName("group-" + UUID.randomUUID())
                        .priorityLevel(PriorityLevel.LOW)
                        .build();
        container.enableLocalThroughputControlGroup(groupConfig);

        CosmosItemRequestOptions options = new CosmosItemRequestOptions();
        options.setThroughputControlGroupName(groupConfig.getGroupName());
for(int i=1;i<=10;i++) {
    System.out.println("\n\n\n");
    CosmosItemResponse<Food> item = container.readItem("05001",
            new PartitionKey("Poultry Products"),
            options,
            Food.class);

    double requestCharge = item.getRequestCharge();
    Duration requestLatency = item.getDuration();

    LOGGER.info("Item successfully read with id {} with a charge of {} within duration {} activity-id {}",
            item.getItem().getId(), requestCharge, requestLatency, item.getActivityId());
    System.out.println("\n\n\n");
}
        //
        System.exit(0);
    }
}
