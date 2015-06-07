package org.devconferences.elastic;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.PutMapping;
import org.devconferences.events.EventsRepository;
import org.elasticsearch.common.settings.ImmutableSettings;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import static org.devconferences.env.EnvUtils.fromEnv;

/**
 * Created by chris on 08/06/15.
 */
public class Elastic {
    public static final String ES_URL = "ES_URL";
    public static final String DEV_CONFERENCES_INDEX = "dev-conferences";

    public static RuntimeJestClient createClient() {
        String esURL = fromEnv(ES_URL, "http://localhost:9200");

        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(esURL)
                .multiThreaded(true)
                .build());
        return new RuntimeJestClientAdapter(factory.getObject());
    }

    public static void createIndexIfNotExists() throws IOException {
        JestClient client = createClient();

        IndicesExists indicesExists = new IndicesExists.Builder(DEV_CONFERENCES_INDEX).build();
        JestResult indexExistsResult = client.execute(indicesExists);
        boolean found = indexExistsResult.getJsonObject().getAsJsonPrimitive("found").getAsBoolean();

        if (!found) {
            System.out.println("Creating index : " + DEV_CONFERENCES_INDEX);
            CreateIndex createIndex =
                    new CreateIndex.Builder(DEV_CONFERENCES_INDEX)
                            .settings(ImmutableSettings.settingsBuilder().build().getAsMap())
                            .build();
            client.execute(createIndex);

            //createMapping(EventsRepository.CITIES_TYPE, "/elastic/cities-mapping.json");
            createMapping(EventsRepository.EVENTS_TYPE, "/elastic/events-mapping.json");
        }
    }

    private static void createMapping(String type, String mappingFilePath) throws IOException {
        JestClient client = createClient();

        String mappingFile = new String(Files.readAllBytes(FileSystems.getDefault().getPath(EventsRepository.class.getResource(mappingFilePath).getPath())));

        PutMapping putMapping = new PutMapping.Builder(DEV_CONFERENCES_INDEX, type, mappingFile).build();
        client.execute(putMapping);
    }

}
