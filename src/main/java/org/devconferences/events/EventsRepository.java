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
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.events.search.PaginatedSearchResult;
import org.devconferences.events.search.CalendarEventSearch;
import org.devconferences.events.search.CompletionSearch;
import org.devconferences.events.search.EventSearch;
import org.devconferences.users.User;
import org.devconferences.users.UsersRepository;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.index.query.*;
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

import static org.devconferences.elastic.ElasticUtils.DEV_CONFERENCES_INDEX;
import static org.devconferences.elastic.ElasticUtils.createClient;
import static org.elasticsearch.common.unit.DistanceUnit.KILOMETERS;
import static org.elasticsearch.index.query.FilterBuilders.geoDistanceFilter;
import static org.elasticsearch.index.query.FilterBuilders.rangeFilter;
import static org.elasticsearch.index.query.QueryBuilders.*;

final class SuggestResponse {
    public Suggests suggest;

    public class Suggests {
        public List<SuggestDataList> cityEventSuggest;
        public List<SuggestDataList> nameEventSuggest;
        public List<SuggestDataList> tagsEventSuggest;
        public List<SuggestDataList> nameCalendarSuggest;

    }

    public class SuggestDataList {
        public List<SuggestData> options;
    }
}

@Singleton
public class EventsRepository {
    public static final String EVENTS_TYPE = "events";
    public static final String CALENDAREVENTS_TYPE = "calendarevents";

    private static final double GEO_DISTANCE = 20d;

    private final RuntimeJestClient client;

    public EventsRepository() {
        this(createClient());
    }

    public EventsRepository(RuntimeJestClient client) {
        this.client = client;
    }

    public void indexOrUpdate(Object obj) {
        String updateStringTemplate = "{" +
                "  \"doc\": %s," +
                "  \"detect_noop\": true," +
                "  \"doc_as_upsert\" : true" +
                "}";
        Get get;
        Update update;
        Percolate percolate;
        String message;

        // Prepare queries (get, update, percolate, depends of class)
        if(obj instanceof CalendarEvent) {
            ESCalendarEvents esCalendarEvents = new ESCalendarEvents((CalendarEvent) obj);
            esCalendarEvents.name_calendar_suggest.input = Arrays.asList(esCalendarEvents.name.split(" "));

            String updateString = String.format(updateStringTemplate,
                    new Gson().toJson(esCalendarEvents));
            update = new Update.Builder(updateString).index(DEV_CONFERENCES_INDEX)
                    .type(CALENDAREVENTS_TYPE).id(esCalendarEvents.id).build();
            get = new Get.Builder(DEV_CONFERENCES_INDEX, esCalendarEvents.id).type(CALENDAREVENTS_TYPE).build();
            percolate = new Percolate.Builder(DEV_CONFERENCES_INDEX, CALENDAREVENTS_TYPE, updateString).build();
        } else if(obj instanceof Event) {
            ESEvents esEvents = new ESEvents((Event) obj);
            esEvents.name_event_suggest.input = Arrays.asList(esEvents.name.split(" "));
            esEvents.tags_event_suggest.input = esEvents.tags;
            esEvents.city_event_suggest.input = esEvents.city;

            String updateString = String.format(updateStringTemplate,
                    new Gson().toJson(esEvents));
            update = new Update.Builder(updateString).index(DEV_CONFERENCES_INDEX)
                    .type(EVENTS_TYPE).id(esEvents.id).build();
            get = new Get.Builder(DEV_CONFERENCES_INDEX, esEvents.id).type(EVENTS_TYPE).build();
            percolate = new Percolate.Builder(DEV_CONFERENCES_INDEX, EVENTS_TYPE, updateString).build();
        } else {
            throw new RuntimeException("Unknown class : " + obj.getClass().getName());
        }

        DocumentResult documentResultGet = client.execute(get);
        DocumentResult documentResultUpdate = client.execute(update);

        boolean founded = documentResultGet.getJsonObject().get("found").getAsBoolean();
        Integer oldVersion = null;
        if(founded) {
            oldVersion = documentResultGet.getJsonObject().get("_version").getAsInt();
        }
        Integer newVersion = documentResultUpdate.getJsonObject().get("_version").getAsInt();

        if(!founded) {
            message = "Document created: " + documentResultUpdate.getId();

        } else if(!oldVersion.equals(newVersion)) {
           message = "Document udpated: " + documentResultUpdate.getId();

        } else {
            // neither update nor creation => no more actions
            return;
        }

        // Find percolators, to notify owners
        UsersRepository usersRepository = new UsersRepository();
        JestResult jestResult = client.execute(percolate);

        List<String> matchedPercolators = getMatchesPercolators(jestResult);
        List<String> ownersPercolators = getPercolatorsOwners(matchedPercolators);
        List<User> users = usersRepository.getUsers(ownersPercolators);

        System.out.println(message);
        System.out.println(matchedPercolators);
        System.out.println(ownersPercolators);
        System.out.println(users);
        users.forEach(user -> {
            System.out.println(user.name());
        });
    }

    private List<String> getMatchesPercolators(JestResult jestResult) {
        List<String> result = new ArrayList<>();
        jestResult.getJsonObject().get("matches").getAsJsonArray().forEach(jsonElement ->
                result.add(jsonElement.getAsJsonObject().get("_id").getAsString()));

        return result;
    }

    private List<String> getPercolatorsOwners(List<String> percolatorsIds) {
        Set<String> owners = new HashSet<>();

        percolatorsIds.forEach(id -> {
            String[] values = id.split("_", 2);
            owners.add(values[0]);
        });

        return owners.stream().collect(Collectors.toList());
    }

    public void createEvent(Event event) {
        if (getEvent(event.id) != null) {
            throw new RuntimeException("Event already exists with same id");
        } else {
            Index index = new Index.Builder(event).index(DEV_CONFERENCES_INDEX).type(EVENTS_TYPE)
                    .id(event.id).build();

            client.execute(index);
        }
    }

    public CompletionSearch suggest(String query, User user) {
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
            Suggest suggest = new Suggest.Builder(XContentHelper.convertToJson(suggestBuilder.buildAsBytes(), false))
                    .addIndex(DEV_CONFERENCES_INDEX).build();

            jestResult = client.execute(suggest);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Suggestions
        CompletionSearch result = new CompletionSearch();
        result.query = query;
        result.hits = new ArrayList<>();

        HashMap<String, Double> rating = new HashMap<>();

        SuggestResponse.Suggests test = new Gson().fromJson(jestResult.getJsonObject(), SuggestResponse.Suggests.class);
        test.cityEventSuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));
        test.nameEventSuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));
        test.tagsEventSuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));
        test.nameCalendarSuggest.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));

        // Add favourites booster in suggestions
        if(user != null) {
            user.favourites.tags.forEach((tag) -> {
                SuggestData suggestData = new SuggestData();
                suggestData.text = tag;
                suggestData.score = 10d;
                if(rating.containsKey(tag) || query == null || query.equals("")) {
                    getSuggestConsumer(suggestData, rating);
                }
            });
        }

        // Create list of suggestions
        rating.forEach((key, value) -> {
            SuggestData item = new SuggestData();
            item.text = key;
            item.score = value;
            result.hits.add(item);
        });

        // Sort all of this : (high score, alphabetical text)
        result.hits.sort((suggO, suggT1) -> {
            if (suggO != null && suggT1 != null) {
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

    // Choose between matchAllQuery and queryStringQuery depending of query
    private QueryBuilder getQueryBuilder(String query) {
        if(query == null || query.equals("") || query.equals("undefined")) {
            return QueryBuilders.matchAllQuery();
        } else {
            return QueryBuilders.queryStringQuery(QueryParser.escape(query));
        }
    }

    public List<CityLight> getAllCitiesWithQuery(String query) {
        SearchSourceBuilder searchQuery = new SearchSourceBuilder();
        searchQuery.size(0);

        QueryBuilder queryBuilder = getQueryBuilder(query);

        searchQuery.query(queryBuilder).aggregation(
                AggregationBuilders.terms("cities").field("city").size(100).subAggregation(
                        AggregationBuilders.terms("types").field("type")
                )
        );

        Search search = new Search.Builder(searchQuery.toString())
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(EVENTS_TYPE)
                .build();

        SearchResult searchResult = client.execute(search);
        if(!searchResult.isSucceeded()) {
            throw new RuntimeException(searchResult.getErrorMessage());
        }

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
            int countForCity = countCalendarEventsAround(query, value.lat(), value.lon(), GEO_DISTANCE);
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
        return getAllCitiesWithQuery(null);
    }

    public City getCity(String cityId, String query) {
        QueryBuilder queryBuilder = getQueryBuilder(query);
        SearchSourceBuilder searchQuery = new SearchSourceBuilder();
        searchQuery.size(ElasticUtils.MAX_SIZE)
                .query(filteredQuery(queryBuilder, FilterBuilders.queryFilter(
                    QueryBuilders.termQuery("city", cityId)
                )));

        City city = new City();
        city.id = cityId;
        city.name = cityId;
        city.communities = new ArrayList<>();
        city.conferences = new ArrayList<>();

        Search search = new Search.Builder(searchQuery.toString())
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(EVENTS_TYPE)
                .build();

        SearchResult searchResult = client.execute(search);
        if(!searchResult.isSucceeded()) {
            throw new RuntimeException(searchResult.getErrorMessage());
        }
        searchResult.getHits(Event.class)
                .stream()
                .map(hit -> hit.source)
                .forEach(event -> addEventToCityObject(city, event));

        city.location = GeopointCities.getInstance().getLocation(city.name);
        if(city.location != null) {
            city.upcoming_events = findCalendarEventsAround(query, city.location.lat(), city.location.lon(), GEO_DISTANCE);
        } else {
            city.upcoming_events = new ArrayList<>();
        }

        return city;
    }

    public Map<String, Long> findEventsAround(double lat, double lon, double distance, int geohashPrecision) {
        String eventLocations = new SearchSourceBuilder()
                .query(filteredQuery(matchAllQuery(),
                        geoDistanceFilter("gps")
                        .distance(distance, KILOMETERS)
                        .lat(lat).lon(lon)
                ))
                .size(0)
                .aggregation(AggregationBuilders.geohashGrid("event_locations").field("gps").precision(geohashPrecision))
                .toString();

        Search search = new Search.Builder(eventLocations)
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(EVENTS_TYPE)
                .build();

        SearchResult result = client.execute(search);
        if(!result.isSucceeded()) {
            throw new RuntimeException(result.getErrorMessage());
        }
        GeoHashGridAggregation locations = result.getAggregations().getGeohashGridAggregation("event_locations");
        return locations.getBuckets().stream().collect(Collectors.toMap(GeoHashGridAggregation.GeoHashGrid::getKey, Bucket::getCount));
    }

    public int countCalendarEventsAround(String query, double lat, double lon, double distance) {
        QueryBuilder queryBuilder = getQueryBuilder(query);
        SearchSourceBuilder eventLocations = new SearchSourceBuilder()
                .query(filteredQuery(queryBuilder,
                        geoDistanceFilter("location.gps")
                        .distance(distance, KILOMETERS)
                        .lat(lat).lon(lon)));

        Count search = new Count.Builder().query(eventLocations.toString())
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(CALENDAREVENTS_TYPE)
                .build();

        CountResult result = client.execute(search);
        if(!result.isSucceeded()) {
            throw new RuntimeException(result.getErrorMessage());
        }
        return result.getCount().intValue();
    }

    public List<CalendarEvent> findCalendarEventsAround(String query, double lat, double lon, double distance) {
        QueryBuilder queryBuilder = getQueryBuilder(query);
        SearchSourceBuilder eventLocations = new SearchSourceBuilder()
                .query(filteredQuery(queryBuilder,
                        geoDistanceFilter("location.gps")
                                .distance(distance, KILOMETERS)
                                .lat(lat).lon(lon)
                ))
                .sort(SortBuilders.fieldSort("date").order(SortOrder.ASC))
                .size(ElasticUtils.MAX_SIZE); // Default max value, or ES will throw an Exception

        Search search = new Search.Builder(eventLocations.toString())
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(CALENDAREVENTS_TYPE)
                .build();

        SearchResult result = client.execute(search);
        if(!result.isSucceeded()) {
            throw new RuntimeException(result.getErrorMessage());
        }
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
        Get get = new Get.Builder(DEV_CONFERENCES_INDEX, eventId).type(EVENTS_TYPE).build();

        JestResult result = client.execute(get);
        if(!result.isSucceeded()) {
            if(result.getResponseCode() == 404) { // Not found : that's not an error
                return null;
            } else {
                throw new RuntimeException(result.getErrorMessage());
            }
        }
        return result.getSourceAsObject(Event.class);
    }


    public EventSearch searchEvents(String query, String page, String limit) {
        return (EventSearch) search(query, page, EVENTS_TYPE, null, null, limit);
    }

    public CalendarEventSearch searchCalendarEvents(String query, String page, String limit) {
        FilterBuilder filterOldCE = rangeFilter("date").gt(System.currentTimeMillis());
        SortBuilder sortByDate = SortBuilders.fieldSort("date").order(SortOrder.ASC);
        return (CalendarEventSearch) search(query, page, CALENDAREVENTS_TYPE, sortByDate, filterOldCE, limit);
    }

    private PaginatedSearchResult search(String query, String page, String typeSearch, SortBuilder sortBy, FilterBuilder filter, String limit) {
        SearchSourceBuilder searchQuery = new SearchSourceBuilder();
        final int pageInt = (page == null || page.equals("undefined") || page.equals("null") ?
                1 : Integer.valueOf(page));
        final int perPage = (limit == null || limit.equals("undefined") || limit.equals("null") ?
                10 : Integer.valueOf(limit));

        // Check parameters
        if (pageInt <= 0) {
            throw new RuntimeException("HTML 400 : page parameter must be positive");
        }
        if (query == null || query.equals("undefined")) {
            throw new RuntimeException("HTML 400 : query parameter is missing");
        }
        if (perPage < 1 || perPage > 1000) {
            throw new RuntimeException("HTML 400 : limit parameter must be between 1 and 1000");
        }

        // Count query
        QueryBuilder queryBuilder = getQueryBuilder(query);

        if (filter == null) {
            searchQuery.query(queryBuilder);
        } else {
            searchQuery.query(filteredQuery(queryBuilder, filter));
        }

        Count count = new Count.Builder().query(searchQuery.toString())
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(typeSearch)
                .build();

        JestResult azerty = client.execute(count);
        CountResult countResult = client.execute(count);
        if(!countResult.isSucceeded()) {
            throw new RuntimeException(countResult.getErrorMessage());
        }

        // Search query
        // Check conditions about page and size (part 2)
        if (perPage * (pageInt - 1) >= countResult.getCount() && (countResult.getCount() != 0 || pageInt != 1)) {
            throw new RuntimeException("HTML 400 : page out of bounds");
        }

        if(sortBy != null) {
            searchQuery.sort(sortBy);
        }

        searchQuery.from(perPage * (pageInt - 1));
        searchQuery.size(perPage);

        Search search = new Search.Builder(searchQuery.toString())
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(typeSearch)
                .build();

        SearchResult searchResult = client.execute(search);
        if(!searchResult.isSucceeded()) {
            throw new RuntimeException(searchResult.getErrorMessage());
        }

        // Create result of search
        PaginatedSearchResult res;

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
        res.currPage = String.valueOf(pageInt);

        int totalPages = (int) Math.ceil(Float.parseFloat(res.totalHits) / (float) perPage);

        res.totalPage = String.valueOf(totalPages);
        res.hitsAPage = String.valueOf(Math.min(perPage, Integer.valueOf(res.totalHits)));

        if(res instanceof EventSearch) {
            res.hits = getHitsFromSearch(searchResult, Event.class);
        } else {
            res.hits = getHitsFromSearch(searchResult, CalendarEvent.class);
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

        Delete delete = new Delete.Builder(eventId).index(DEV_CONFERENCES_INDEX).type(EVENTS_TYPE).build();

        client.execute(delete);
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

        Search search = new Search.Builder(searchQuery.toString())
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(CALENDAREVENTS_TYPE)
                .build();

        SearchResult searchResult = client.execute(search);
        if(!searchResult.isSucceeded()) {
            throw new RuntimeException(searchResult.getErrorMessage());
        }

        return getHitsFromSearch(searchResult, CalendarEvent.class);
    }

    public static List getHitsFromSearch(SearchResult searchResult, Class<?> sourceType) {
        return (searchResult.getHits(sourceType).stream()
                .map((data) -> data.source).collect(Collectors.toList())
        );
    }

    public SearchResult searchByIds(String type, List<String> ids) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        IdsQueryBuilder idsQueryFilter = new IdsQueryBuilder();

        ids.forEach(idsQueryFilter::addIds);

        searchSourceBuilder.query(idsQueryFilter).size(ElasticUtils.MAX_SIZE);

        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(DEV_CONFERENCES_INDEX).addType(type).build();

        return client.execute(search);
    }
}
