package com.sample.cosmos.changefeed;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.sample.cosmos.changefeed.fullfidelity.CFP_FF_Push;
import com.sample.cosmos.client.CosmosClientUtil;

import java.time.Duration;

public class ChangeFeedDataTest {
    public static void main(String[] args) {
        final CosmosAsyncClient client = CosmosClientUtil.getAsyncClient();
        final CosmosAsyncContainer feedContainer = CosmosClientUtil.getAsyncCollection(client);
        //
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    CFP_FF_Push.createNewDocumentsCustomPOJO(feedContainer, 10, Duration.ofSeconds(3));
                    CFP_FF_Push.upsertDocumentsCustomPOJO(feedContainer, 10, Duration.ofSeconds(3));
                    CFP_FF_Push.deleteDocumentsCustomPOJO(feedContainer, 10, Duration.ofSeconds(3));
                    //
                    try {
                        Thread.sleep(1000*60*1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();

    }

}
