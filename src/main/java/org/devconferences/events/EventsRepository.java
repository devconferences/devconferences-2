package org.devconferences.events;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import io.searchbox.client.JestResult;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Suggest;
import io.searchbox.core.search.aggregation.Bucket;
import io.searchbox.core.search.aggregation.GeoHashGridAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.devconferences.elastic.ElasticUtils.createClient;
import static org.elasticsearch.common.unit.DistanceUnit.KILOMETERS;
import static org.elasticsearch.index.query.QueryBuilders.*;

@Singleton
public class EventsRepository {

    private final class SuggestResponse {
        public Suggests suggest;

        public class Suggests {
            public List<SuggestsData> cityEventSuggest;
            public List<SuggestsData> nameEventSuggest;
            public List<SuggestsData> tagsEventSuggest;
            public List<SuggestsData> nameCalendarSuggest;

        }

        public class SuggestsData {
            public List<SuggestData> options;
        }
    }
    public static final String EVENTS_TYPE = "events";
    public static final String CALENDAREVENTS_TYPE = "calendarevents";

    private static final double GEO_DISTANCE = 20d;

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
            calendarEvent.name_calendar_suggest.input = Arrays.asList(calendarEvent.name.split(" "));
            client.indexES(CALENDAREVENTS_TYPE, calendarEvent, calendarEvent.id);
        } else if(obj instanceof Event) {
            Event event = (Event) obj;
            event.name_event_suggest.input = Arrays.asList(event.name.split(" "));
            event.tags_event_suggest.input = event.tags;
            event.city_event_suggest.input = event.city;
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

    public CompletionResult suggest(String query) {
        SuggestBuilder suggestBuilder = new SuggestBuilder();
        suggestBuilder.addSuggestion(
                SuggestBuilders.completionSuggestion("nameEventSuggest")
                        .field("name_event_suggest").text(query)
        ).addSuggestion(
                SuggestBuilders.completionSuggestion("cityEventSuggest")
                        .field("city_event_suggest").text(query)
        ).addSuggestion(
                SuggestBuilders.completionSuggestion("tagsEventSuggest")
                        .field("tags_event_suggest").text(query)
        ).addSuggestion(
                SuggestBuilders.completionSuggestion("nameCalendarSuggest")
                        .field("name_calendar_suggest").text(query)
        );

        JestResult jestResult = new JestResult(new Gson());
        try {
            jestResult = client.execute(new Suggest.Builder(XContentHelper.convertToJson(suggestBuilder.buildAsBytes(), false)).build());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Suggestions
        CompletionResult result = new CompletionResult();
        result.query = query;
        result.hits = new ArrayList<>();

        HashMap<String, Double> rating = new HashMap<>();

        SuggestResponse.Suggests test = new Gson().fromJson(jestResult.getJsonObject(), SuggestResponse.Suggests.class);
        test.cityEventSuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));
        test.nameEventSuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));
        test.tagsEventSuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));
        test.nameCalendarSuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));

        // Create list of suggestions
        rating.forEach((key, value) -> {
            SuggestData item = new SuggestData();
            item.text = key;
            item.score = value;
            result.hits.add(item);
        });

        // Sort all of this : (high score, alphabetical text)
        result.hits.sort((Comparator) (o, t1) -> {
            if (o instanceof SuggestData && t1 instanceof SuggestData) {
                SuggestData suggO = (SuggestData) o;
                SuggestData suggT1 = (SuggestData) t1;

                if(suggO.score.compareTo(suggT1.score) != 0) {
                    return -1 * suggO.score.compareTo(suggT1.score); // Desc sort
                } else {
                    return suggO.text.compareTo(suggT1.text);
                }
            } else {
                return -1;
            }
        });

        return result;
    }

    public List<CityLight> getAllCitiesWithQuery(String query, boolean matchAll) {
        SearchSourceBuilder searchQuery = new SearchSourceBuilder();
        searchQuery.size(0);

        if(matchAll) {
            searchQuery.aggregation(
                    AggregationBuilders.terms("cities").field("city").size(100).subAggregation(
                            AggregationBuilders.terms("types").field("type")
                    )
            );
        } else {
            searchQuery.query(QueryBuilders.queryStringQuery(query)).aggregation(
                    AggregationBuilders.terms("cities").field("city").size(100).subAggregation(
                            AggregationBuilders.terms("types").field("type")
                    )
            );
        }

        SearchResult searchResult = client.searchES(EVENTS_TYPE, searchQuery.toString());

        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation cities = aggregations.getAggregation("cities", TermsAggregation.class);

        HashMap<String,CityLight> resultMap = new HashMap<>();

        for (TermsAggregation.Entry city : cities.getBuckets()) {
            TermsAggregation types = city.getTermsAggregation("types");
            CityLight cityLight = new CityLight(city.getKey(), city.getKey());
            cityLight.location = GeopointCities.getInstance().getLocation(city.getKey());
            for (TermsAggregation.Entry type : types.getBuckets()) {
                switch (type.getKey()) {
                    case "COMMUNITY":
                        cityLight.totalCommunity = type.getCount();
                        break;
                    case "CONFERENCE":
                        cityLight.totalConference = type.getCount();
                        break;
                }
            }

            resultMap.put(city.getKey(), cityLight);
        }

        // Attach each CalendarEvent with its city
        GeopointCities.getInstance().getAllLocations().forEach((key, value) -> {
            int countForCity = countCalendarEventsAround((matchAll ? null : query), value.lat(), value.lon(), GEO_DISTANCE);
            if(countForCity > 0) {
                if (!resultMap.containsKey(key)) {
                    CityLight cityLight = new CityLight(key, key);
                    cityLight.location = value;
                    resultMap.put(key, cityLight);
                }
                resultMap.get(key).totalCalendar += countForCity;
            }
        });

        // HashMap -> List
        return resultMap.values().stream().map((city) -> {
            city.count = city.totalCalendar + city.totalCommunity + city.totalConference;
            return city;
        }).sorted().collect(Collectors.toList());
    }

    @Deprecated
    public List<CityLight> getAllCities() {
        return getAllCitiesWithQuery(null, true);
    }

    public City getCity(String cityId) {
        SearchSourceBuilder searchQuery = new SearchSourceBuilder();
        searchQuery.size(ElasticUtils.MAX_SIZE)
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
            city.upcoming_events = findCalendarEventsAround(null, city.location.lat(), city.location.lon(), GEO_DISTANCE);
        } else {
            city.upcoming_events = new ArrayList<>();
        }

        return city;
    }

    public Map<String, Long> findEventsAround(double lat, double lon, double distance, int geohashPrecision) {
        String eventLocations = new SearchSourceBuilder()
                .query(QueryBuilders.geoDistanceQuery("gps")
                        .distance(distance, KILOMETERS)
                        .lat(lat).lon(lon)
                )
                .size(0)
                .aggregation(AggregationBuilders.geohashGrid("event_locations").field("gps").precision(geohashPrecision))
                .toString();

        SearchResult result = client.searchES(EVENTS_TYPE, eventLocations);
        GeoHashGridAggregation locations = result.getAggregations().getGeohashGridAggregation("event_locations");
        return locations.getBuckets().stream().collect(Collectors.toMap(GeoHashGridAggregation.GeoHashGrid::getKey, Bucket::getCount));
    }

    public int countCalendarEventsAround(String query, double lat, double lon, double distance) {
        SearchSourceBuilder eventLocations = new SearchSourceBuilder()
                .query(matchAllQuery())
                .size(0)
                .postFilter(QueryBuilders.geoDistanceQuery("location.gps")
                        .distance(distance, KILOMETERS)
                        .lat(lat).lon(lon));

        if(query != null) {
            eventLocations.query(QueryBuilders.queryStringQuery(query));
        }

        SearchResult result = client.searchES(CALENDAREVENTS_TYPE, eventLocations.toString());
        return result.getTotal();
    }

    public List<CalendarEvent> findCalendarEventsAround(String query, double lat, double lon, double distance) {
        SearchSourceBuilder eventLocations = new SearchSourceBuilder()
                .postFilter(QueryBuilders.geoDistanceQuery("location.gps")
                        .distance(distance, KILOMETERS)
                        .lat(lat).lon(lon))
                .sort(SortBuilders.fieldSort("date").order(SortOrder.ASC))
                .size(ElasticUtils.MAX_SIZE); // Default max value, or ES will throw an Exception

        if(query != null) {
            eventLocations.query(QueryBuilders.queryStringQuery(query));
        }

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


    public EventSearch searchEvents(String query, String page, String lat, String lon, String distance, Boolean all) {
        return (EventSearch) search(query, page, EVENTS_TYPE, null, null, lat, lon, distance, all);
    }

    public CalendarEventSearch searchCalendarEvents(String query, String page, String lat, String lon, String distance, Boolean all) {
        QueryBuilder filterOldCE = QueryBuilders.rangeQuery("date").gt(System.currentTimeMillis());
        SortBuilder sortByDate = SortBuilders.fieldSort("date").order(SortOrder.ASC);
        return (CalendarEventSearch) search(query, page, CALENDAREVENTS_TYPE, sortByDate, filterOldCE, lat, lon, distance, all);
    }

    // If page = "0", return ALL matches events
    private AbstractSearchResult search(String query, String page, String typeSearch, SortBuilder sortBy, QueryBuilder filter, String lat, String lon, String distance, Boolean allMatch) {
        SearchSourceBuilder searchQuery = new SearchSourceBuilder();
        int pageInt = Integer.decode(page);
        final int perPage = 10;

        // Count query
        searchQuery.size(0);
        if (allMatch == null || !allMatch) {
            if (filter == null) {
                searchQuery.query(queryStringQuery(QueryParser.escape(query)));
            } else {
                searchQuery.query(queryStringQuery(QueryParser.escape(query))).postFilter(filter);
            }
        }

        SearchResult countResult = client.searchES(typeSearch, searchQuery.toString());

        // Search query
        // Check conditions about page and size
        if (pageInt <= 0) {
            throw new RuntimeException("HTML 400 : page parameter is <= 0");
        }
        if(allMatch != null && allMatch && pageInt != 1) {
            throw new RuntimeException("HTML 400 : 'all' parameter is true and page is not equals to 1");
        }
        if (perPage * (pageInt - 1) >= countResult.getTotal() && (countResult.getTotal() != 0 || pageInt != 1)) {
            throw new RuntimeException("HTML 400 : page out of bounds");
        }

        if(sortBy != null) {
            searchQuery.sort(sortBy);
        }

        if (allMatch) {
            searchQuery.size(countResult.getTotal());
        } else {
            searchQuery.from(perPage * (pageInt - 1));
            searchQuery.size(perPage);
        }

        // Suggestions
        // Disable when query is empty / null...
        if(query != null && !query.equals("")) {
            switch (typeSearch) {
                case EVENTS_TYPE:
                    searchQuery.suggest().addSuggestion(
                            SuggestBuilders.completionSuggestion("citySuggest").text(query).field("city_event_suggest")
                    ).addSuggestion(
                            SuggestBuilders.completionSuggestion("nameSuggest").text(query).field("name_event_suggest")
                    ).addSuggestion(
                            SuggestBuilders.completionSuggestion("tagsSuggest").text(query).field("tags_event_suggest")
                    );
                    break;
                case CALENDAREVENTS_TYPE:
                    searchQuery.suggest().addSuggestion(
                            SuggestBuilders.completionSuggestion("nameSuggest").text(query).field("name_calendar_suggest")
                    );
                    break;
                default:
                    throw new RuntimeException("Unknown search type : " + typeSearch);

            }
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

//        // Suggestions
//        res.suggests = new ArrayList<>();
//
//        HashMap<String, Double> rating = new HashMap<>();
//
//        SuggestResponse test = new Gson().fromJson(searchResult.getJsonObject(), SuggestResponse.class);
//        if(test.suggest != null) {
//            switch (typeSearch) {
//                case EVENTS_TYPE:
//                    // Merge 3 suggests list, and add scores if a suggest appears at least twice
//                    test.suggest.citySuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));
//                    test.suggest.nameSuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));
//                    test.suggest.tagsSuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));
//                    break;
//                case CALENDAREVENTS_TYPE:
//                    test.suggest.nameSuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));
//            }
//
//            // Create list of suggestions
//            rating.forEach((key, value) -> {
//                AbstractSearchResult.SuggestData item = res.new SuggestData();
//                item.text = key;
//                item.score = value;
//                res.suggests.add(item);
//            });
//
//            // Sort all of this : (high score, alphabetical text)
//            res.suggests.sort((Comparator) (o, t1) -> {
//                if (o instanceof AbstractSearchResult.SuggestData && t1 instanceof AbstractSearchResult.SuggestData) {
//                    AbstractSearchResult.SuggestData suggO = (AbstractSearchResult.SuggestData) o;
//                    AbstractSearchResult.SuggestData suggT1 = (AbstractSearchResult.SuggestData) t1;
//
//                    if(suggO.score.compareTo(suggT1.score) != 0) {
//                        return -1 * suggO.score.compareTo(suggT1.score); // Desc sort
//                    } else {
//                        return suggO.text.compareTo(suggT1.text);
//                    }
//                } else {
//                    return -1;
//                }
//            });
//        }

        int totalPages = (int) Math.ceil(Float.parseFloat(res.totalHits) / (float) perPage);

        res.totalPage = String.valueOf(totalPages);
        res.hitsAPage = (!allMatch ? String.valueOf(perPage) : String.valueOf(res.totalHits));

        if(res instanceof EventSearch) {
            EventSearch resCast = (EventSearch) res;
            resCast.hits = getHitsFromSearch(searchResult, Event.class);
        } else if(res instanceof CalendarEventSearch) {
            CalendarEventSearch resCast = (CalendarEventSearch) res;
            resCast.hits = getHitsFromSearch(searchResult, CalendarEvent.class);
        }

        return res;
    }

    private void getSuggestConsumer(SuggestData suggest, HashMap<String, Double> rating) {
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

        int pageInt = ElasticUtils.MAX_SIZE;
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
