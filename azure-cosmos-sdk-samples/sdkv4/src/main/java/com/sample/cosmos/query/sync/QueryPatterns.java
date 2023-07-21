package com.sample.cosmos.query.sync;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.JsonNode;
import com.sample.cosmos.client.CosmosClientUtil;
import com.sample.cosmos.vo.Food;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.stream.Collectors;

/*
Request Unit considerations

	Item size: As the size of an item increases, the number of RUs consumed to read or write the item also increases.

	Item indexing: By default, each item is automatically indexed. Fewer RUs are consumed if you choose not to index some of your items in a container.

	Item property count: Assuming the default indexing is on all properties, the number of RUs consumed to write an item increases as the item property count increases.

	Indexed properties: An index policy on each container determines which properties are indexed by default. To reduce the RU consumption for write operations, limit the number of indexed properties.

	Data consistency: The strong and bounded staleness consistency levels consume approximately two times more RUs while performing read operations when compared to that of other relaxed consistency levels.

	Type of reads: Point reads cost significantly fewer RUs than queries.

	Query patterns: The complexity of a query affects how many RUs are consumed for an operation. Factors that affect the cost of query operations include:
		The number of query results
		The number of predicates
		The nature of the predicates
		The number of user-defined functions
		The size of the source data
		The size of the result set

 */

// Data Model is key here (Embed, Reference or Hybrid)
/*
// Embed (denormalized) Patient with address and contact details
    Data from entities is queried together
    Child data is dependent on a parent
    1:1 relationship
    Similar rate of updates – does the data change at the same pace
    1:few – the set of values is bounded

    Usually embedding provides better read performance
    Follow-above to minimize trade-off for write perf

// Reference (Blog and Comments) (Person and Stocks)
    1 : many (unbounded relationship)
    many : many relationships
    Data changes at different rates
    What is referenced, is heavily referenced by many others

    Typically provides better write performance
    But may require more network calls(round trips) for reads

// Hybrid Model
    Data from other entities embedded in top-level document and other data is reference

 */


/*
Read operations in Azure Cosmos DB are typically ordered from fastest/most efficient to slower/less efficient in terms of RU consumption as follows:
    Point reads (key/value lookup on a single item ID and partition key).
    Query with a filter clause within a single partition key.
    Query without an equality or range filter clause on any property.
    Query without filters.

The RU cost of writing an item depends on:
    The item size.
    The number of properties covered by the indexing policy and needed to be indexed.

Optimizing writes - The best way to optimize the RU cost of write operations is to rightsize your items and the number of properties that get indexed.
 */

/*
 the number of Azure Cosmos DB items loaded/returned,
 the number of lookups against the index,
 the query compilation time etc. details.
 */
public class QueryPatterns {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryPatterns.class);
    public static boolean QUERY_DIAGNOSTICS_FLAG = true;
    public static int ITEM_COUNT = 10;

    public static void main(String... args) {
        CosmosClient client = CosmosClientUtil.getClient();
        CosmosContainer container = CosmosClientUtil.getCollection(client
//                , CosmosClientUtil.COLLECTION_WITH_CUSTOM_INDEX);
                , CosmosClientUtil.COLLECTION);
                //, CosmosClientUtil.COLLECTION_WITH_NO_INDEX);
        //
        writeRecord(container, "Seafood", "212313");
        //pointread(container, "Poultry Products", "05001");
        //pointread(container, "Cereal Grains and Pasta", "120002"); //large 1MB document
        //queryInPartitionWithId(container, "Poultry Products", "05001");
        //queryInPartitionWithId(container, "Cereal Grains and Pasta", "120002");
        //queryInPartitionWithId(container, "Poultry Products");
        queryCrossPartition(container, "Chicken");
//        queryUsingCompositeIndex(container);
        //
        System.exit(0);
    }

    // The only factor affecting the RU charge of a point read (besides the consistency level used) is the size of the item retrieved.
    public static void pointread(final CosmosContainer container, final String pkey, final String id) {
        try {
            CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();

            System.out.println("\n\n\n");
            CosmosItemResponse<Food> item = container.readItem(id, new PartitionKey(pkey), Food.class);

            // This was introduced
            //container.readMany(new ArrayList<>(), Food.class);

            double requestCharge = item.getRequestCharge();
            Duration requestLatency = item.getDuration();

            LOGGER.info("Item successfully read with id {} with a charge of {} within duration {} activity-id {}",
                    item.getItem().getId(), requestCharge, requestLatency, item.getActivityId());
            System.out.println("\n\n\n");
        } catch (CosmosException e) {
            LOGGER.error("Read Item failed with", e);
        }
    }

    //In order to be an in-partition query, the query must have an equality filter that includes the partition key
    public static void queryInPartitionWithId(final CosmosContainer container, final String pkey, final String id) {
        System.out.println("\n\n\n");
        // Set some common query options
        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        //queryOptions.setEnableCrossPartitionQuery(true); //No longer necessary in SDK v4
        //  Set query metrics enabled to get metrics around query executions
        queryOptions.setQueryMetricsEnabled(true);

        CosmosPagedIterable<Food> familiesPagedIterable = container.queryItems(
                "SELECT * FROM Food WHERE Food.id = '"+id+"' and Food.foodGroup = '"+pkey+"' ", queryOptions, Food.class);

        familiesPagedIterable.iterableByPage(ITEM_COUNT).forEach(cosmosItemPropertiesFeedResponse -> {
            if(QUERY_DIAGNOSTICS_FLAG)
                LOGGER.info(cosmosItemPropertiesFeedResponse.getCosmosDiagnostics().toString());
            LOGGER.info("Got a page of query result with {} items(s), request charge of {}, activity-id {}",
                    cosmosItemPropertiesFeedResponse.getResults().size(),
                    cosmosItemPropertiesFeedResponse.getRequestCharge(),
                    cosmosItemPropertiesFeedResponse.getActivityId());

            cosmosItemPropertiesFeedResponse.getContinuationToken();
            LOGGER.info("Item Ids {}", cosmosItemPropertiesFeedResponse
                    .getResults()
                    .stream()
                    .map(Food::getId)
                    .collect(Collectors.toList()));
        });
        System.out.println("\n\n\n");
    }

    //In order to be an in-partition query, the query must have an equality filter that includes the partition key
    public static void queryInPartitionWithId(final CosmosContainer container, final String pkey) {
        System.out.println("\n\n\n");
        // Set some common query options
        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        //queryOptions.setEnableCrossPartitionQuery(true); //No longer necessary in SDK v4
        //  Set query metrics enabled to get metrics around query executions
        queryOptions.setQueryMetricsEnabled(true);
        queryOptions.setMaxBufferedItemCount(ITEM_COUNT);

        /*CosmosPagedIterable<Food> familiesPagedIterable = container.queryItems(
                "SELECT * FROM Food WHERE  Food.foodGroup = '"+pkey+"' ", queryOptions, Food.class);*/
        CosmosPagedIterable<Food> familiesPagedIterable = container.readAllItems(new PartitionKey(pkey), Food.class);

        familiesPagedIterable.iterableByPage(ITEM_COUNT).forEach(cosmosItemPropertiesFeedResponse -> {
            if(QUERY_DIAGNOSTICS_FLAG)
                LOGGER.info(cosmosItemPropertiesFeedResponse.getCosmosDiagnostics().toString());
            LOGGER.info("Got a page of query result with {} items(s), request charge of {}, activity-id {}",
                    cosmosItemPropertiesFeedResponse.getResults().size(),
                    cosmosItemPropertiesFeedResponse.getRequestCharge(),
                    cosmosItemPropertiesFeedResponse.getActivityId());
            //
            LOGGER.info("Item Ids {}", cosmosItemPropertiesFeedResponse
                    .getResults()
                    .stream()
                    .map(Food::getId)
                    .collect(Collectors.toList()));
        });
        System.out.println("\n\n\n");
    }

    public static void queryCrossPartition(final CosmosContainer container, final String keyword) {
        System.out.println("\n\n\n");
        // Set some common query options
        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        //queryOptions.setEnableCrossPartitionQuery(true); //No longer necessary in SDK v4
        //  Set query metrics enabled to get metrics around query executions
        // queryOptions.setQueryMetricsEnabled(true);
        // ets the maximum number of simultaneous network connections to the container's partitions. If you set this
        // property to -1, the SDK manages the degree of parallelism. If the MaxConcurrency set to 0, there is
        // a single network connection to the container's partitions.
        // “maxDegreeOfParallelism”=-1 means Cosmos DB handles the degree of parallelism.
        // “maxDegreeOfParallelism”=0 means there is a single network connection and will query the partition one by one in series.
        // “maxDegreeOfParallelism” >0 means multiple partitions will be queried in parallel.
        queryOptions.setMaxDegreeOfParallelism(3);
        // Trades query latency versus client-side memory utilization. If this option is omitted or to set to -1,
        // the SDK manages the number of items buffered during parallel query execution.
        queryOptions.setMaxBufferedItemCount(ITEM_COUNT);
        //queryOptions.setPartitionKey(new PartitionKey("Poultry Products"));

        CosmosPagedIterable<Food> familiesPagedIterable = container.queryItems(
//                "SELECT * FROM Food WHERE  CONTAINS(Food.description,'"+keyword+"') ", queryOptions, Food.class);
                "SELECT * FROM Food WHERE  Food.isFromSurvey = true ", queryOptions, Food.class);
//                "SELECT * FROM Food WHERE  Food.isFromSurvey = true order by Food.isFromSurvey, Food.foodGroup, Food.id ", queryOptions, Food.class); // requires composite index
//                "SELECT * FROM Food WHERE  Food.isFromSurvey = true and Food.foodGroup in ('Poultry Products', 'Fats and Oils') ", queryOptions, Food.class);
//        "SELECT * FROM Food WHERE  Food.isFromSurvey = true and Food.foodGroup in ('Fast Foods', 'Fats and Oils') ", queryOptions, Food.class);

        familiesPagedIterable.iterableByPage(ITEM_COUNT).forEach(cosmosItemPropertiesFeedResponse -> {
            //
            if(QUERY_DIAGNOSTICS_FLAG)
                LOGGER.info(cosmosItemPropertiesFeedResponse.getCosmosDiagnostics().toString());
            //
            LOGGER.info("Got a page of query result with {} items(s), request charge of {}, activity-id {}, session-token {}",
                    cosmosItemPropertiesFeedResponse.getResults().size(),
                    cosmosItemPropertiesFeedResponse.getRequestCharge(),
                    cosmosItemPropertiesFeedResponse.getActivityId(),
                    cosmosItemPropertiesFeedResponse.getSessionToken());
            //
            LOGGER.info("Item Ids {}", cosmosItemPropertiesFeedResponse
                    .getResults()
                    .stream()
                    .map(Food::getId)
                    .collect(Collectors.toList()));
        });
        System.out.println("\n\n\n");
    }

    /*
    1. Filter expressions can use multiple composite indexes.
    2. The properties in the query's filter should match those in composite index. If a property is in the composite index but isn't included in the query as a filter, the query won't utilize the composite index.
    3. If a query has other properties in the filter that aren't defined in a composite index, then a combination of composite and range indexes will be used to evaluate the query. This will require fewer RUs than exclusively using range indexes.
    4. If a property has a range filter (>, <, <=, >=, or !=), then this property should be defined last in the composite index. If a query has more than one range filter, it may benefit from multiple composite indexes.
    5. When creating a composite index to optimize queries with multiple filters, the ORDER of the composite index will have no impact on the results. This property is optional.
     */
    public static void queryUsingCompositeIndex(final CosmosContainer container) {
        System.out.println("\n\n\n");
        // Set some common query options
        CosmosQueryRequestOptions queryOptions = new CosmosQueryRequestOptions();
        //queryOptions.setEnableCrossPartitionQuery(true); //No longer necessary in SDK v4
        //  Set query metrics enabled to get metrics around query executions
        // queryOptions.setQueryMetricsEnabled(true);
        // ets the maximum number of simultaneous network connections to the container's partitions. If you set this
        // property to -1, the SDK manages the degree of parallelism. If the MaxConcurrency set to 0, there is
        // a single network connection to the container's partitions.
        // “maxDegreeOfParallelism”=-1 means Cosmos DB handles the degree of parallelism.
        // “maxDegreeOfParallelism”=0 means there is a single network connection and will query the partition one by one in series.
        // “maxDegreeOfParallelism” >0 means multiple partitions will be queried in parallel.
        queryOptions.setMaxDegreeOfParallelism(3);
        // Trades query latency versus client-side memory utilization. If this option is omitted or to set to -1,
        // the SDK manages the number of items buffered during parallel query execution.
        queryOptions.setMaxBufferedItemCount(ITEM_COUNT);
        //queryOptions.setPartitionKey(new PartitionKey("Poultry Products"));

        CosmosPagedIterable<Food> familiesPagedIterable = container.queryItems(
                "SELECT * FROM Food  WHERE Food.version > 1 order by Food.weightInGrams, Food.amount, Food.nutritionValue ", queryOptions, Food.class); // requires composite index
//                "SELECT * FROM Food WHERE Food.description = 'Amaranth grain, uncooked' and Food.amount = 1 order by Food.weightInGrams, Food.amount, Food.nutritionValue ", queryOptions, Food.class); // requires composite index

        familiesPagedIterable.iterableByPage(ITEM_COUNT).forEach(cosmosItemPropertiesFeedResponse -> {
            //
            if(QUERY_DIAGNOSTICS_FLAG)
                LOGGER.info(cosmosItemPropertiesFeedResponse.getCosmosDiagnostics().toString());
            //
            LOGGER.info("Got a page of query result with {} items(s), request charge of {}, activity-id {}",
                    cosmosItemPropertiesFeedResponse.getResults().size(),
                    cosmosItemPropertiesFeedResponse.getRequestCharge(),
                    cosmosItemPropertiesFeedResponse.getActivityId());
            //
            LOGGER.info("Item Ids {}", cosmosItemPropertiesFeedResponse
                    .getResults()
                    .stream()
                    .map(Food::getId)
                    .collect(Collectors.toList()));
        });
        System.out.println("\n\n\n");
    }

    // Write RU would depend on the size of the document, index size, consistency etc.
    public static void writeRecord(final CosmosContainer container, final String pkey, final String id) {
        try {
            //
            Food food = new Food();
            food.setId(id);
            food.setFoodGroup(pkey);
            food.setNutritionValue(BigDecimal.TEN);
            //
            System.out.println("\n\n\n");
            CosmosItemResponse<Food> item = container.upsertItem(food, new CosmosItemRequestOptions());
            double requestCharge = item.getRequestCharge();
            Duration requestLatency = item.getDuration();

            LOGGER.info("Item successfully write with id {} with a charge of {} within duration {} activity-id {} session-token {}",
                    item.getItem().getId(), requestCharge, requestLatency, item.getActivityId(), item.getSessionToken());
            System.out.println("\n\n\n");
        } catch (CosmosException e) {
            LOGGER.error("Read Item failed with", e);
        }
    }
}
