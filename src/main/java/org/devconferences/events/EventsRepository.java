package org.devconferences.events;

import com.google.common.base.Preconditions;
import io.searchbox.client.JestResult;
import io.searchbox.core.*;
import io.searchbox.core.search.aggregation.Bucket;
import io.searchbox.core.search.aggregation.GeoHashGridAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.devconferences.elastic.RuntimeJestClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.devconferences.elastic.ElasticUtils.createClient;
import static org.elasticsearch.common.unit.DistanceUnit.KILOMETERS;
import static org.elasticsearch.index.query.QueryBuilders.*;

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
        SearchSourceBuilder searchQuery = new SearchSourceBuilder();
        searchQuery.size(0).aggregation(
                AggregationBuilders.terms("cities")
                        .field("city")
                        .size(100)
                        .order(Terms.Order.term(true))
        );

        SearchResult searchResult = client.searchES(EVENTS_TYPE, searchQuery.toString());

        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation cities = aggregations.getAggregation("cities", TermsAggregation.class);

        return cities.getBuckets().stream()
                .map(entry -> new CityLight(entry.getKey(), entry.getKey(), entry.getCount(),
                        GeopointCities.getInstance().getLocation(entry.getKey())))
                .collect(Collectors.toList());
    }

    public City getCity(String cityId) {
        SearchSourceBuilder searchQuery = new SearchSourceBuilder();
        searchQuery.size(Integer.MAX_VALUE)
                .query(QueryBuilders.termQuery("city", cityId));

        City city = new City();
        city.id = cityId;
        city.name = cityId;
        city.communities = new ArrayList<>();
        city.conferences = new ArrayList<>();

        SearchResult searchResult = client.searchES(EVENTS_TYPE, searchQuery.toString());
        searchResult.getHits(Event.class)
                .stream()
                .map(hit -> hit.source)
                .forEach(event -> addEventToCityObject(city, event));

        return city;
    }

    public Map<String, Long> findEventsAround(double lat, double lon, double distance, int geohashPrecision) {
        String eventLocations = new SearchSourceBuilder()
                .query(QueryBuilders.geoDistanceQuery("location")
                        .distance(distance, KILOMETERS)
                        .lat(lat).lon(lon)
                )
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


    public EventSearch searchEvents(String query, String page, String lat, String lon, String distance) {
        return (EventSearch) search(query, page, EVENTS_TYPE, null, null, lat, lon, distance);
    }

    public CalendarEventSearch searchCalendarEvents(String query, String page, String lat, String lon, String distance) {
        QueryBuilder filterOldCE = QueryBuilders.rangeQuery("date").gt(System.currentTimeMillis());
        SortBuilder sortByDate = SortBuilders.fieldSort("date").order(SortOrder.ASC);
        return (CalendarEventSearch) search(query, page, CALENDAREVENTS_TYPE, sortByDate, filterOldCE, lat, lon, distance);
    }

    // If page = "0", return ALL matches events
    private AbstractSearchResult search(String query, String page, String typeSearch, SortBuilder sortBy, QueryBuilder filter, String lat, String lon, String distance) {
        SearchSourceBuilder searchQuery = new SearchSourceBuilder();
        int pageInt = Integer.decode(page);

        // Count query
        if (filter == null) {
            searchQuery.query(queryStringQuery(QueryParser.escape(query)));
        } else {
            searchQuery.query(filter);
        }

        CountResult countResult = client.countES(typeSearch, searchQuery.toString());

        // Search query
        if (pageInt < 0) {
            throw new RuntimeException("Can't search with a negative page");
        }

        if(sortBy != null) {
            searchQuery.sort(sortBy);
        }

        if(!page.equals("0")) {
            searchQuery.from(10 * (pageInt - 1));
            searchQuery.size(10);
        } else {
            searchQuery.size(countResult.getCount().intValue());
        }


        SearchResult searchResult = client.searchES(typeSearch, searchQuery.toString());

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
        res.lat = lat;
        res.lon = lon;
        res.distance = distance;

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
        SearchSourceBuilder searchQuery = new SearchSourceBuilder();

        int pageInt = Integer.MAX_VALUE;
        if(page != null && Integer.decode(page) > 0) {
            pageInt = Integer.decode(page);
        }

        searchQuery.size(pageInt)
                .sort(SortBuilders.fieldSort("date").order(SortOrder.ASC))
                .query(QueryBuilders.rangeQuery("date").gt(System.currentTimeMillis()
                ));

        SearchResult searchResult = client.searchES(CALENDAREVENTS_TYPE, searchQuery.toString());

        return getHitsFromSearch(searchResult, CalendarEvent.class);
    }

    private List getHitsFromSearch(SearchResult searchResult, Class<?> sourceType) {
        return (StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(searchResult.getHits(sourceType).iterator(), Spliterator.ORDERED),
                false).map(hitResult -> hitResult.source).collect(Collectors.toList())
        );
    }
}
