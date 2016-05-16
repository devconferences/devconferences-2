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
            indexES(EVENTS_TYPE, event, event.id);
        }
    }

    /** All function with an ES call **/

    public void indexES(String type, Object event, String id) {
        Index index = new Index.Builder(event).index(DEV_CONFERENCES_INDEX).type(type).id(id).build();

        client.execute(index);
    }

    private SearchResult searchES(String type, String query) {
        Search search = new Search.Builder(query)
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(type)
                .build();

        return client.execute(search);
    }

    private CountResult countES(String type, String query) {
        Count count = new Count.Builder()
                .query(query)
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(type)
                .build();

        return client.execute(count);
    }

    private JestResult getES(String type, String id) {
        Get get = new Get.Builder(DEV_CONFERENCES_INDEX, id).type(type).build();

        return client.execute(get);
    }

    private void deleteES(String type, String id) {
        Delete delete = new Delete.Builder(id).index(DEV_CONFERENCES_INDEX).type(type).build();

        client.execute(delete);
    }

    /** End of ES call section **/

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

        SearchResult searchResult = searchES(EVENTS_TYPE, allCitiesQuery);

        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation cities = aggregations.getAggregation("cities", TermsAggregation.class);

        return cities.getBuckets().stream()
                .map(entry -> new CityLight(entry.getKey(), entry.getKey(), entry.getCount()))
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

        City city = new City();
        city.id = cityId;
        city.name = cityId;
        city.communities = new ArrayList<>();
        city.conferences = new ArrayList<>();

        SearchResult searchResult = searchES(EVENTS_TYPE, query);
        searchResult.getHits(Event.class)
                .stream()
                .map(hit -> hit.source)
                .forEach(event -> addEventToCityObject(city, event));

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

        SearchResult result = searchES(EVENTS_TYPE, eventLocations);
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
        JestResult result = getES(EVENTS_TYPE, eventId);
        return result.getSourceAsObject(Event.class);
    }

    // If page = "0", return ALL matches events
    public EventSearch search(String query, String page) {
        String matchAllQueryPart1 = "" +
                "{";
        String matchAllQueryPart2 = "" +
                "   \"query\": {" +
                "      \"query_string\": {" +
                "         \"query\" : \"" + query + "\"" +
                "       }" +
                "   }" +
                "}";
        int pageInt = Integer.decode(page);
        int size = 10;

        CountResult countResult = countES(EVENTS_TYPE, matchAllQueryPart1 + matchAllQueryPart2);

        // Prepare search query
        String querySearch;
        if(pageInt == 0) {
            querySearch = matchAllQueryPart1 +
                    "   \"size\": " + countResult.getCount().intValue() + "," +
                    matchAllQueryPart2;
        } else if (pageInt > 0) {
            querySearch = matchAllQueryPart1 +
                    "   \"size\": " + size + "," +
                    "   \"from\": " + ((pageInt - 1) * 10) + "," +
                    matchAllQueryPart2;
        } else {
            throw new RuntimeException("Can't search events with a negative page");
        }

        SearchResult searchResult = searchES(EVENTS_TYPE, querySearch);

        // Create result of search
        EventSearch res = new EventSearch();
        res.totalHits = String.valueOf(countResult.getCount().intValue());
        res.query = query;
        res.currPage = String.valueOf(page);

        int totalPages;
        if(pageInt > 0) {
            totalPages = (int) Math.ceil(Float.parseFloat(res.totalHits) / 10.0f);
        } else {
            totalPages = 0;
        }

        res.totalPage = String.valueOf(totalPages);
        res.hitsAPage = (pageInt > 0 ? "10" : String.valueOf(res.totalHits));
        res.hits = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(searchResult.getHits(Event.class).iterator(), Spliterator.ORDERED),
                false).map(hitResult -> hitResult.source).collect(Collectors.toList());

        return res;
    }

    public void deleteEvent(String eventId) {
        Preconditions.checkArgument(!eventId.equals(""));

        deleteES(EVENTS_TYPE, eventId);
    }
}
