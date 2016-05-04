package org.devconferences.events;

import com.google.common.base.Preconditions;
import io.searchbox.client.JestResult;
import io.searchbox.core.*;
import io.searchbox.core.search.aggregation.Bucket;
import io.searchbox.core.search.aggregation.GeoHashGridAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.devconferences.elastic.RuntimeJestClient;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.devconferences.elastic.ElasticUtils.DEV_CONFERENCES_INDEX;
import static org.devconferences.elastic.ElasticUtils.createClient;
import static org.elasticsearch.common.unit.DistanceUnit.KILOMETERS;
import static org.elasticsearch.index.query.FilterBuilders.geoDistanceFilter;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

@Singleton
public class EventsRepository {
    public static final String EVENTS_TYPE = "events";

    public final RuntimeJestClient client;

    public EventsRepository() {
        client = createClient();
    }

    public void createEvent(Event event) {
        if (getEvent(event.id) != null) {
            throw new RuntimeException("Event already exists with same id");
        } else {
            indexOrUpdate(event);
        }
    }

    public void indexOrUpdate(Event event) {
        Index index = new Index.Builder(event).index(DEV_CONFERENCES_INDEX).type(EVENTS_TYPE).id(event.id).build();
        client.execute(index);
    }

    public List<CityLight> getAllCities() {
        String allCitiesQuery = "" +
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

        SearchResult searchResult = client.execute(search);

        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation cities = aggregations.getAggregation("cities", TermsAggregation.class);

        return cities.getBuckets().stream()
                .map(entry -> {
                    return new CityLight(entry.getKey(), entry.getKey(), entry.getCount());
                })
                .collect(Collectors.toList());

    }

    public City getCity(String cityId) {
        String query = "" +
                "{" +
                "  \"query\": {" +
                "    \"term\": {" +
                "      \"city\": {" +
                "        \"value\": \"" + cityId + "\"" +
                "      }" +
                "    }" +
                "  }," +
                "  \"size\" : " + Integer.MAX_VALUE +
                "}";

        Search search = new Search.Builder(query)
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(EVENTS_TYPE).build();

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


    }

    public Map<String, Long> findEventsAround(double lat, double lon, double distance, int geohashPrecision) {
        String eventLocations = new SearchSourceBuilder()
                .query(filteredQuery(matchAllQuery(),
                        geoDistanceFilter("location")
                                .distance(distance, KILOMETERS)
                                .lat(lat).lon(lon)
                ))
                .size(0)
                .aggregation(AggregationBuilders.geohashGrid("event_locations").field("location").precision(geohashPrecision))
                .toString();

        SearchResult result = client.execute(new Search.Builder(eventLocations).build());
        GeoHashGridAggregation locations = result.getAggregations().getGeohashGridAggregation("event_locations");
        return locations.getBuckets().stream().collect(Collectors.toMap(GeoHashGridAggregation.GeoHashGrid::getKey, Bucket::getCount));
    }

    private void addEventToCityObject(City city, Event event) {
        switch (event.type) {
            case COMMUNITY:
                city.communities.add(event);
                break;
            case CONFERENCE:
                city.conferences.add(event);
                break;
            default:
                throw new IllegalStateException("Unknown type : " + event.type);
        }
    }

    public Event getEvent(String eventId) {
        Get get = new Get.Builder(DEV_CONFERENCES_INDEX, eventId).type(EVENTS_TYPE).build();

        JestResult result = client.execute(get);
        return result.getSourceAsObject(Event.class);
    }

    public List<Event> search(String query) {
        String matchAllQueryPart1 = "" +
                "{";
        String matchAllQueryPart2 = "" +
                "   \"query\": {" +
                "      \"query_string\": {" +
                "         \"query\" : \"" + query + "\"" +
                "       }" +
                "   }" +
                "}";

        Count count = new Count.Builder()
                .query(matchAllQueryPart1 + matchAllQueryPart2)
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(EVENTS_TYPE)
                .build();

        CountResult countResult = client.execute(count);

        Search search = new Search.Builder(matchAllQueryPart1 +
                    "   \"size\": " + countResult.getCount().intValue() + "," +
                    matchAllQueryPart2)
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(EVENTS_TYPE)
                .build();

        SearchResult searchResult = client.execute(search);

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(searchResult.getHits(Event.class).iterator(), Spliterator.ORDERED),
                false).map(hitResult -> hitResult.source).collect(Collectors.toList());


    }

    public void deleteEvent(String eventId) {
        Preconditions.checkArgument(!eventId.equals(""));

        Delete delete = new Delete.Builder(eventId).index(DEV_CONFERENCES_INDEX).type(EVENTS_TYPE).build();
        client.execute(delete);
    }


}
