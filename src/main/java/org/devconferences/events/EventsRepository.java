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

import static org.devconferences.elastic.ElasticUtils.createClient;
import static org.elasticsearch.common.unit.DistanceUnit.KILOMETERS;
import static org.elasticsearch.index.query.FilterBuilders.geoDistanceFilter;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

@Singleton
public class EventsRepository {
    public static final String EVENTS_TYPE = "events";
    public static final String CALENDAREVENTS_TYPE = "calendarevents";

    public final RuntimeJestClient client;

    public EventsRepository() {
        this(createClient());
    }

    public EventsRepository(RuntimeJestClient client) {
        this.client = client;
    }

    public void indexOrUpdate(Object obj) {
        if(obj instanceof CalendarEvent) {
            CalendarEvent calendarEvent = (CalendarEvent) obj;
            client.indexES(CALENDAREVENTS_TYPE, calendarEvent, calendarEvent.id);
        } else if(obj instanceof Event) {
            Event event = (Event) obj;
            client.indexES(EVENTS_TYPE, event, event.id);
        } else {
            throw new RuntimeException("Unknown class : " + obj.getClass().getName());
        }
    }

    public void createEvent(Event event) {
        if (getEvent(event.id) != null) {
            throw new RuntimeException("Event already exists with same id");
        } else {
            client.indexES(EVENTS_TYPE, event, event.id);
        }
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

        SearchResult searchResult = client.searchES(EVENTS_TYPE, allCitiesQuery);

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

        SearchResult searchResult = client.searchES(EVENTS_TYPE, query);
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

        SearchResult result = client.searchES(EVENTS_TYPE, eventLocations);
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
        JestResult result = client.getES(EVENTS_TYPE, eventId);
        return result.getSourceAsObject(Event.class);
    }


    public EventSearch searchEvents(String query, String page) {
        return (EventSearch) search(query, page, EVENTS_TYPE, null, false, null);
    }

    public CalendarEventSearch searchCalendarEvents(String query, String page) {
        String filterOldCE = "" +
                "        \"range\" : {" +
                "          \"date\" : { \"gt\" : " + System.currentTimeMillis() + " }" +
                "        }";
        return (CalendarEventSearch) search(query, page, CALENDAREVENTS_TYPE, "date", false, filterOldCE);
    }

    // If page = "0", return ALL matches events
    private AbstractSearchResult search(String query, String page, String typeSearch, String sortBy, boolean isDesc, String filter) {
        // template for search and count queries
        String sqRootOpen      = "{";
        String sqSizeFormat    = "  \"size\": %s,";
        String sqFromFormat    = "  \"from\": %s,";
        String sqSortOpen      = "  \"sort\": [";
        String sqSortFormat    = "    {\"%s\": \"%s\"}";
        String sqSortClose     = "  ],";
        String sqQueryOpen     = "  \"query\": {";
        // Query : No filter : only this
        String sqQueryString   = "    \"query_string\": {" +
                                 "      \"query\": \"%s\"" +
                                 "    }";
        // Query : With filter : add all of this
        String sqFilteredOpen  = "    \"filtered\": {";
        String sqFilterOpen    = "      \"filter\": {";
        // Custom filter here
        String sqFilterClose   = "      },";
        String sqFilterQuery   = "      \"query\": {%s}"; // %s = sqQueryString
        String sqFilteredClose = "    }";
        // Query : END
        String sqQueryClose    = "  }";
        String sqRootClose     = "}";
        // ...
        String sqQueryFilter;
        String sqQuery;
        String sqSize;
        String sqFrom;
        String sqSort;
        // ...
        String sqCountString;
        String sqFullString;
        // ...
        int pageInt = Integer.decode(page);

        // Count query
        if (filter == null) {
            sqQuery = sqQueryOpen + String.format(sqQueryString, query.replace("\"", "")) + sqQueryClose;
        } else {
            sqQueryFilter = sqFilteredOpen + sqFilterOpen +
                    filter +
                    sqFilterClose +
                    String.format(sqFilterQuery, String.format(sqQueryString, query.replace("\"", ""))) +
                    sqFilteredClose;
            sqQuery = sqQueryOpen + sqQueryFilter + sqQueryClose;
        }

        sqCountString = sqRootOpen + sqQuery + sqRootClose;
        CountResult countResult = client.countES(typeSearch, sqCountString);

        // Search query
        if (pageInt < 0) {
            throw new RuntimeException("Can't search with a negative page");
        }

        sqSort = sortBy == null ? "" : sqSortOpen +
                String.format(sqSortFormat, sortBy, (isDesc ? "desc" : "asc")) +
                sqSortClose;
        sqFrom = (page.equals("0") ? "" : String.format(sqFromFormat, 10 * (pageInt - 1)));
        sqSize = String.format(sqSizeFormat, (page.equals("0") ? countResult.getCount().intValue() : 10));
        sqFullString = sqRootOpen +
                sqSize + sqFrom + sqSort + sqQuery +
                sqRootClose;

        SearchResult searchResult = client.searchES(typeSearch, sqFullString);

        // Create result of search
        AbstractSearchResult res;

        switch(typeSearch) {
            case EVENTS_TYPE:
                res = new EventSearch();
            break;
            case CALENDAREVENTS_TYPE:
                res = new CalendarEventSearch();
            break;
            default:
                throw new RuntimeException("Unknown search type : " + typeSearch);
        }

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

        if(res instanceof EventSearch) {
            EventSearch resCast = (EventSearch) res;
            resCast.hits = getHitsFromSearch(searchResult, Event.class);
        } else if(res instanceof CalendarEventSearch) {
            CalendarEventSearch resCast = (CalendarEventSearch) res;
            resCast.hits = getHitsFromSearch(searchResult, CalendarEvent.class);
        }

        return res;
    }

    public void deleteEvent(String eventId) {
        Preconditions.checkNotNull(eventId, "Should not be null !");
        Preconditions.checkArgument(!eventId.equals(""));

        client.deleteES(EVENTS_TYPE, eventId);
    }

    public List<CalendarEvent> getCalendarEvents(String page) {
        int pageInt = Integer.MAX_VALUE;
        if(page != null && Integer.decode(page) > 0) {
            pageInt = Integer.decode(page);
        }
        String query = "" +
                "{" +
                "  \"size\" : " + pageInt + "," +
                "  \"sort\" : [" +
                "    {\"date\" : \"asc\"}" +
                "  ]," +
                "  \"query\" : {" +
                "    \"filtered\" : {" +
                "      \"filter\" : {" + // filter old events
                "        \"range\" : {" +
                "          \"date\" : { \"gt\" : " + System.currentTimeMillis() + " }" +
                "        }" +
                "      }" +
                "    }" +
                "  }" +
                "}";

        SearchResult searchResult = client.searchES(CALENDAREVENTS_TYPE, query);

        return getHitsFromSearch(searchResult, CalendarEvent.class);
    }

    public List getHitsFromSearch(SearchResult searchResult, Class<?> sourceType) {
        return (StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(searchResult.getHits(sourceType).iterator(), Spliterator.ORDERED),
                false).map(hitResult -> hitResult.source).collect(Collectors.toList())
        );
    }
}
