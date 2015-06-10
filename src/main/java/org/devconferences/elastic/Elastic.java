package org.devconferences.elastic;

import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.PutMapping;
import org.apache.commons.io.IOUtils;
import org.devconferences.events.EventsRepository;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.devconferences.env.EnvUtils.fromEnv;

/**
 * Created by chris on 08/06/15.
 */
public class Elastic {
    public static final String ES_URL = "ES_URL";
    public static final String DEV_CONFERENCES_INDEX = "dev-conferences";
    private static final Logger LOGGER = LoggerFactory.getLogger(Elastic.class);


    public static RuntimeJestClient createClient() {
        String esURL = fromEnv(ES_URL, "http://localhost:9200");

        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(esURL)
                .multiThreaded(true)
                .build());
        return new RuntimeJestClientAdapter(factory.getObject());
    }

    public static void createIndexIfNotExists() {
        RuntimeJestClient client = createClient();

        try {
            IndicesExists indicesExists = new IndicesExists.Builder(DEV_CONFERENCES_INDEX).build();
            JestResult indexExistsResult = client.execute(indicesExists);
            boolean found = indexExistsResult.getJsonObject().getAsJsonPrimitive("found").getAsBoolean();

            if (!found) {
                LOGGER.info("Creating index : " + DEV_CONFERENCES_INDEX);
                CreateIndex createIndex =
                        new CreateIndex.Builder(DEV_CONFERENCES_INDEX)
                                .settings(ImmutableSettings.settingsBuilder().build().getAsMap())
                                .build();
                JestResult jestResult = client.execute(createIndex);
                if (!jestResult.isSucceeded()) {
                    throw new IllegalStateException("Index creation failed : " + jestResult.getJsonString());
                }
                createMapping(EventsRepository.EVENTS_TYPE, "/elastic/events-mapping.json");
            }
        } finally {
            client.shutdownClient();
        }
    }

    private static void createMapping(String type, String mappingFilePath) {
        RuntimeJestClient client = createClient();

        try {
            String mappingFile;
            try {
                mappingFile = IOUtils.toString(EventsRepository.class.getResourceAsStream(mappingFilePath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            PutMapping putMapping = new PutMapping.Builder(DEV_CONFERENCES_INDEX, type, mappingFile).build();
            client.execute(putMapping);
        } finally {
            client.shutdownClient();
        }
    }

}
