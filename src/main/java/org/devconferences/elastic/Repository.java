package org.devconferences.elastic;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Get;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.PutMapping;
import org.devconferences.v1.City;
import org.elasticsearch.common.settings.ImmutableSettings;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by chris on 02/06/15.
 */
@Singleton
public class Repository {
    public static final String DEV_CONFERENCES_INDEX = "dev-conferences";
    public static final String CITIES_TYPE = "cities";
    private static final String ES_URL = "ES_URL";

    public final JestClient client;
    private int matchAllSize = 100;

    public Repository() {
        String esURL = System.getenv(ES_URL) != null ? System.getenv(ES_URL) : "http://localhost:9200";

        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(esURL)
                .multiThreaded(true)
                .build());
        client = factory.getObject();
    }

    public JestClient getClient() {
        return client;
    }

    public void indexCity(City city) {

        Index index = new Index.Builder(city).index(DEV_CONFERENCES_INDEX).type(CITIES_TYPE).id(city.id).build();
        try {
            client.execute(index);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<City> getAllCities() {
        String matchAllQuery = "{\n" +
                "   \"query\": {\n" +
                "      \"match_all\": {}\n" +
                "   },\n" +
                "   \"size\": " + matchAllSize + "\n" +
                "}";

        Search search = new Search.Builder(matchAllQuery)
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(CITIES_TYPE)
                .build();

        SearchResult searchResult;
        try {
            searchResult = client.execute(search);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        int totalHits = searchResult.getTotal();
        if (totalHits > matchAllSize) {
            // si le matchAllSize est trop faible, on l'augmente
            matchAllSize = totalHits;
            return getAllCities();
        } else {
            return StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(searchResult.getHits(City.class).iterator(), Spliterator.ORDERED),
                    false).map(hitResult -> hitResult.source).collect(Collectors.toList());
        }
    }

    public City getCity(String id) {
        System.out.println("Getting : " + id);
        Get get = new Get.Builder(DEV_CONFERENCES_INDEX, id).type(CITIES_TYPE).build();

        JestResult result;
        try {
            result = client.execute(get);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result.getSourceAsObject(City.class);
    }

    public void createIndexIfNotFound() throws IOException {
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

            String citiesMapping = new String(Files.readAllBytes(FileSystems.getDefault().getPath(Repository.class.getResource("/elastic/cities.json").getPath())));

            PutMapping putMapping = new PutMapping.Builder(DEV_CONFERENCES_INDEX, Repository.CITIES_TYPE, citiesMapping).build();
            client.execute(putMapping);
        }

    }
}
