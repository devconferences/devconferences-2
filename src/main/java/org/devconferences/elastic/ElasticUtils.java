package org.devconferences.elastic;

import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.DeleteMapping;
import io.searchbox.indices.mapping.PutMapping;
import org.apache.commons.io.IOUtils;
import org.devconferences.events.EventsRepository;
import org.devconferences.users.UsersRepository;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.devconferences.env.EnvUtils.fromEnv;

/**
 * Created by chris on 08/06/15.
 */
public final class ElasticUtils {
    public static final int MAX_SIZE = 10000; // Default max value, or ES will throw an Exception

    private ElasticUtils() {

    }

    public static final String ES_URL = "ES_URL";
    public static final String DEV_CONFERENCES_INDEX = "dev-conferences";
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticUtils.class);
    
    public static RuntimeJestClient createClient() {
        if(DeveloppementESNode.portNode != null) {
            // Set port to "0" disable creation of client (useful in tests)
            if(DeveloppementESNode.portNode.equals("0")) {
                return null;
            } else {
                return createClient(DeveloppementESNode.portNode);
            }
        } else {
            return createClient(DeveloppementESNode.elasticPort);
        }
    }

    public static RuntimeJestClient createClient(String port) {
        String esURL = fromEnv(ES_URL, "http://localhost:" + port);

        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(esURL)
                .multiThreaded(true)
                .readTimeout(10000) // Default is 3000, but sometimes a SockettimeoutException might threw
                .build());
        return new RuntimeJestClientAdapter(factory.getObject());
    }

    public static void createIndex() {
        createIndexIfNotExists();
    }

    public static void createIndexIfNotExists() {
        try (RuntimeJestClient client = createClient();) {
            JestResult indexExistsResult = indiceExists(client, DEV_CONFERENCES_INDEX);
            boolean found = indexExistsResult.isSucceeded();

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
                createAllTypes(false);
            }
        } catch (NullPointerException e) {
            LOGGER.warn("No RuntimeJestClient have been created !");
        }
    }

    public static void createAllTypes(boolean deleteAllTypes) {
        if(deleteAllTypes) {
            deleteAllTypes();
        }
        createType(EventsRepository.EVENTS_TYPE, "/elastic/events-mapping.json");
        createType(EventsRepository.CALENDAREVENTS_TYPE, "/elastic/calendarevents-mapping.json");
        createType(UsersRepository.USERS_TYPE, "/elastic/users-mapping.json");
    }

    public static void deleteAllTypes() {
        try (RuntimeJestClient client = createClient();) {
            DeleteMapping deleteMapping = new DeleteMapping.Builder(DEV_CONFERENCES_INDEX, "*").build();
            JestResult jestResult = client.execute(deleteMapping);
            if (!jestResult.isSucceeded()) {
                throw new IllegalStateException("Can't delete all types : " + jestResult.getErrorMessage());
            }
            LOGGER.info("All types have been deleted");
        }
    }

    static JestResult indiceExists(RuntimeJestClient client, String type) {
        IndicesExists indicesExists = new IndicesExists.Builder(type).build();
        return client.execute(indicesExists);
    }

    private static void createType(String type, String mappingFilePath) {
        try (RuntimeJestClient client = createClient();) {
            String mappingFile;
            try {
                mappingFile = IOUtils.toString(EventsRepository.class.getResourceAsStream(mappingFilePath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            PutMapping putMapping = new PutMapping.Builder(DEV_CONFERENCES_INDEX, type, mappingFile).build();
            JestResult jestResult = client.execute(putMapping);
            if(!jestResult.isSucceeded()) {
                throw new IllegalStateException("Can't create type '" + type + "' : " + jestResult.getErrorMessage());
            }
            LOGGER.info("Type '" + type + "' have been created");
        } catch (NullPointerException e) {
            LOGGER.warn("No RuntimeJestClient have been created !");
        }
    }

    public static void deleteIndex() {
        try (RuntimeJestClient client = createClient();) {
            JestResult indexExistsResult = indiceExists(client, DEV_CONFERENCES_INDEX);
            boolean found = indexExistsResult.isSucceeded();

            if (found) {
                LOGGER.info("Deleting index : " + DEV_CONFERENCES_INDEX);
                DeleteIndex deleteIndex = new DeleteIndex.Builder(DEV_CONFERENCES_INDEX).build();
                JestResult jestResult = client.execute(deleteIndex);
                if (!jestResult.isSucceeded()) {
                    throw new IllegalStateException("Index deletion failed : " + jestResult.getJsonString());
                }
            }
        } catch (NullPointerException e) {
            LOGGER.warn("No RuntimeJestClient have been created !");
        }
    }

}
