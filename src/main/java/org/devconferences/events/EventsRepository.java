package org.devconferences.events;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import io.searchbox.client.JestResult;
import io.searchbox.core.*;
import io.searchbox.core.search.aggregation.Bucket;
import io.searchbox.core.search.aggregation.GeoHashGridAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Collector;
import org.devconferences.elastic.RuntimeJestClient;
import org.elasticsearch.action.suggest.SuggestAction;
import org.elasticsearch.action.suggest.SuggestRequest;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

import javax.inject.Singleton;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.devconferences.elastic.ElasticUtils.DEV_CONFERENCES_INDEX;
import static org.devconferences.elastic.ElasticUtils.createClient;
import static org.elasticsearch.common.unit.DistanceUnit.KILOMETERS;
import static org.elasticsearch.index.query.QueryBuilders.*;

@Singleton
public class EventsRepository {
    private final class SuggestResponse {
        public Suggests suggest;

        public class Suggests {
            public List<SuggestsData> citySuggest;
            public List<SuggestsData> nameSuggest;
            public List<SuggestsData> tagsSuggest;

            public class SuggestsData {
                public List<AbstractSearchResult.Suggest> options;
            }
        }

    }
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
            calendarEvent.name_suggest.input = Arrays.asList(calendarEvent.name.split(" "));
            client.indexES(CALENDAREVENTS_TYPE, calendarEvent, calendarEvent.id);
        } else if(obj instanceof Event) {
            Event event = (Event) obj;
            event.name_suggest.input = Arrays.asList(event.name.split(" "));
            event.tags_suggest.input = event.tags;
            event.city_suggest.input = event.city;
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

        city.location = GeopointCities.getInstance().getLocation(city.name);
        if(city.location != null) {
            city.upcoming_events = findCalendarEventsAround(city.location.lat(), city.location.lon(), 20d);
        } else {
            city.upcoming_events = new ArrayList<>();
        }

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

    public List<CalendarEvent> findCalendarEventsAround(double lat, double lon, double distance) {
        SearchSourceBuilder eventLocations = new SearchSourceBuilder()
                .query(matchAllQuery())
                .postFilter(QueryBuilders.geoDistanceQuery("location.gps")
                        .distance(distance, KILOMETERS)
                        .lat(lat).lon(lon))
                .sort(SortBuilders.fieldSort("date").order(SortOrder.ASC))
                .size(Integer.MAX_VALUE);

        SearchResult result = client.searchES(CALENDAREVENTS_TYPE, eventLocations.toString());
        return getHitsFromSearch(result, CalendarEvent.class);
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
        searchQuery.size(0);
        if (filter == null) {
            searchQuery.query(queryStringQuery(QueryParser.escape(query)));
        } else {
            searchQuery.query(queryStringQuery(QueryParser.escape(query))).postFilter(filter);
        }

        SearchResult countResult = client.searchES(typeSearch, searchQuery.toString());

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
            searchQuery.size(countResult.getTotal());
        }

        // Suggestions
        switch(typeSearch) {
            case EVENTS_TYPE:
                searchQuery.suggest().addSuggestion(
                        SuggestBuilders.completionSuggestion("citySuggest").text(query).field("city_suggest")
                ).addSuggestion(
                        SuggestBuilders.completionSuggestion("nameSuggest").text(query).field("name_suggest")
                ).addSuggestion(
                        SuggestBuilders.completionSuggestion("tagsSuggest").text(query).field("tags_suggest")
                );
                break;
            case CALENDAREVENTS_TYPE:
                searchQuery.suggest().addSuggestion(
                        SuggestBuilders.completionSuggestion("nameSuggest").text(query).field("name_suggest")
                );
                break;
            default:
                throw new RuntimeException("Unknown search type : " + typeSearch);

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

        res.totalHits = String.valueOf(countResult.getTotal());
        res.query = query;
        res.currPage = String.valueOf(page);
        res.lat = lat;
        res.lon = lon;
        res.distance = distance;

        // Suggestions
        res.suggests = new ArrayList<>();

        HashMap<String, Double> rating = new HashMap<>();

        SuggestResponse test = new Gson().fromJson(searchResult.getJsonObject(), SuggestResponse.class);
        if(test.suggest != null) {
            switch (typeSearch) {
                case EVENTS_TYPE:
                    // Merge 3 suggests list, and add scores if a suggest appears at least twice
                    test.suggest.citySuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));
                    test.suggest.nameSuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));
                    test.suggest.tagsSuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));
                    break;
                case CALENDAREVENTS_TYPE:
                    test.suggest.nameSuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));
            }

            // Create list of suggestions
            rating.forEach((key, value) -> {
                AbstractSearchResult.Suggest item = res.new Suggest();
                item.text = key;
                item.score = value;
                res.suggests.add(item);
            });

            // Sort all of this : (high score, alphabetical text)
            res.suggests.sort((Comparator) (o, t1) -> {
                if (o instanceof AbstractSearchResult.Suggest && t1 instanceof AbstractSearchResult.Suggest) {
                    AbstractSearchResult.Suggest suggO = (AbstractSearchResult.Suggest) o;
                    AbstractSearchResult.Suggest suggT1 = (AbstractSearchResult.Suggest) t1;

                    if(suggO.score.compareTo(suggT1.score) != 0) {
                        return -1 * suggO.score.compareTo(suggT1.score); // Desc sort
                    } else {
                        return suggO.text.compareTo(suggT1.text);
                    }
                } else {
                    return -1;
                }
            });
        }

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

    private void getSuggestConsumer(AbstractSearchResult.Suggest suggest, HashMap<String, Double> rating) {
            if(rating.containsKey(suggest.text)) {
                rating.replace(suggest.text, suggest.score + rating.get(suggest.text));
            } else {
                rating.put(suggest.text, suggest.score);
            }
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
