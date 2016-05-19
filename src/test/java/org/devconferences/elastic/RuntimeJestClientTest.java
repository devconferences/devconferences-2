package org.devconferences.elastic;

import io.searchbox.client.JestResult;
import io.searchbox.core.CountResult;
import io.searchbox.core.SearchResult;
import org.assertj.core.api.Assertions;
import org.devconferences.events.Event;
import org.devconferences.events.EventsRepository;
import org.devconferences.jobs.ImportEventsJob;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RuntimeJestClientTest {
    private static RuntimeJestClient jestClient;

    @BeforeClass
    public static void classSetUp() {
        DeveloppementESNode.createDevNode("9250");
        ElasticUtils.clientPort = "9250";
        jestClient = ElasticUtils.createClient();
        Assertions.assertThat(jestClient).isNotNull();
        // Index should not exists yet
        Assertions.assertThat(jestClient.countES(EventsRepository.EVENTS_TYPE, "{}").getJsonString())
                .contains("IndexMissingException[[dev-conferences] missing]");

        ElasticUtils.createIndex(); // Index + type creation
    }

    @Before
    public void setUp() {
        (new ImportEventsJob()).reloadData(true);

        try { // Need waiting few seconds, or loaded data won't be founded...
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCount() {
        CountResult countResult = jestClient.countES(EventsRepository.EVENTS_TYPE, "{" +
                "  \"query\":{" +
                "    \"match_all\" : {}" +
                "  }" +
                "}");

        Assertions.assertThat(countResult.getCount()).isEqualTo(2);
    }

    @Test
    public void testGet() {
        JestResult getResult = jestClient.getES(EventsRepository.EVENTS_TYPE, "testevent");
        // NotNull <=> Founded
        Assertions.assertThat(getResult.getSourceAsObject(Event.class)).isNotNull();
    }

    @Test
    public void testSearch() {
        SearchResult searchResult = jestClient.searchES(EventsRepository.EVENTS_TYPE, "{" +
                "  \"query\":{" +
                "    \"match_all\" : {}" +
                "  }" +
                "}");

        Assertions.assertThat(searchResult.getHits(Event.class)).hasSize(2);
    }

    @Test
    public void testDelete() {
        jestClient.deleteES(EventsRepository.EVENTS_TYPE, "testevent");

        try { // Need waiting few seconds, or changes won't be applied...
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SearchResult searchResult = jestClient.searchES(EventsRepository.EVENTS_TYPE, "{" +
                "  \"query\":{" +
                "    \"match_all\" : {}" +
                "  }" +
                "}");
        Assertions.assertThat(searchResult.getHits(Event.class)).hasSize(1);
        searchResult = jestClient.searchES(EventsRepository.EVENTS_TYPE, "{" +
                "  \"query\":{" +
                "    \"term\" : {" +
                "      \"id\" : \"testevent2\"" +
                "    }" +
                "  }" +
                "}");
        Assertions.assertThat(searchResult.getHits(Event.class)).hasSize(1);

        (new ImportEventsJob()).reloadData(true);
    }

}
