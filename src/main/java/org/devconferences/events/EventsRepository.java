package org.devconferences.events;

import io.searchbox.client.JestClient;
import io.searchbox.core.Delete;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.devconferences.events.City;
import org.devconferences.events.CityLight;
import org.devconferences.events.Event;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.devconferences.elastic.Elastic.DEV_CONFERENCES_INDEX;
import static org.devconferences.elastic.Elastic.createClient;

@Singleton
public class EventsRepository {
    public static final String EVENTS_TYPE = "events";

    public final JestClient client;

    public EventsRepository() {
        client = createClient();
    }

    public void indexEvent(Event event) {
        Index index = new Index.Builder(event).index(DEV_CONFERENCES_INDEX).type(EVENTS_TYPE).id(event.id).build();
        try {
            client.execute(index);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<CityLight> getAllCities() {
        String allCitiesQuery =
                "{" +
                        "  \"size\": 0," +
                        "  \"aggs\": {" +
                        "    \"cities\": {" +
                        "      \"terms\": {" +
                        "        \"field\": \"city\"," +
                        "        \"size\": 100," +
                        "        \"order\": {" +
                        "          \"_term\": \"asc\"" +
                        "        }" +
                        "      }" +
                        "    }" +
                        "  }" +
                        "}";

        Search search = new Search.Builder(allCitiesQuery)
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(EVENTS_TYPE)
                .build();

        try {
            SearchResult searchResult = client.execute(search);

            MetricAggregation aggregations = searchResult.getAggregations();
            TermsAggregation cities = aggregations.getAggregation("cities", TermsAggregation.class);

            return cities.getBuckets().stream()
                    .map(entry -> {
                        return new CityLight(entry.getKey(), entry.getKey(), entry.getCount());
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public City getCity(String cityId) {
        String query =
                "{" +
                        "  \"query\": {" +
                        "    \"term\": {" +
                        "      \"city\": {" +
                        "        \"value\": \"" + cityId + "\"" +
                        "      }" +
                        "    }" +
                        "  }" +
                        "}";

        Search search = new Search.Builder(query)
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(EVENTS_TYPE).build();

        try {
            City city = new City();
            city.id = cityId;
            city.name = cityId;
            city.communities = new ArrayList<>();
            city.conferences = new ArrayList<>();

            SearchResult searchResult = client.execute(search);
            searchResult.getHits(Event.class)
                    .stream()
                    .map(hit -> {
                        return hit.source;
                    })
                    .forEach(event -> {
                        addEventToCityObject(city, event);
                    });

            return city;
        } catch (IOException e) {
            throw new RuntimeException();
        }

    }

    private void addEventToCityObject(City city, Event event) {
        switch (event.type) {
            case COMMUNITY:
                city.conferences.add(event);
                break;
            case CONFERENCE:
                city.communities.add(event);
                break;
            default:
                throw new IllegalStateException("Unknown type : " + event.type);
        }
    }

    public List<Event> search(String query) {
        String matchAllQuery = "{" +
                "   \"query\": {" +
                "      \"query_string\": {" +
                "           \"query\" : \"" + query + "\"" +
                "       }" +
                "   }" +
                "}";

        Search search = new Search.Builder(matchAllQuery)
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(EVENTS_TYPE)
                .build();

        try {
            SearchResult searchResult = client.execute(search);

            return StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(searchResult.getHits(Event.class).iterator(), Spliterator.ORDERED),
                    false).map(hitResult -> hitResult.source).collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public void deleteEvent(Event event) {
        Delete delete = new Delete.Builder(event.id).index(DEV_CONFERENCES_INDEX).type(EVENTS_TYPE).build();

        try {
            client.execute(delete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
