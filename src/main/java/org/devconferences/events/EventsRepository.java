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

@Singleton
public class EventsRepository {
    public static final String EVENTS_TYPE = "events";
    public static final String CALENDAREVENTS_TYPE = "calendarevents";

    private static final double GEO_DISTANCE = 20d;

    private final RuntimeJestClient client;
    private int updateCounter;
    private final UsersRepository usersRepository;

    public EventsRepository() {
        this(createClient(), new UsersRepository());
    }

    public EventsRepository(RuntimeJestClient client) {
        this.client = client;
        this.updateCounter = 1;
        this.usersRepository = new UsersRepository();
    }

    public EventsRepository(RuntimeJestClient client, UsersRepository usersRepository) {
        this.client = client;
        this.updateCounter = 1;
        this.usersRepository = usersRepository;
    }

    // ********************** Event and CalendarEvent ********************** //

    /**
     *
     * @return true if the document was updated
     */
    public boolean indexOrUpdate(Object obj) {
        String updateStringTemplate = "{" +
                "  \"doc\": %s," +
                "  \"detect_noop\": true," +
                "  \"doc_as_upsert\" : true" +
                "}";
        Get get;
        Update update;
        Percolate percolate;

        // Prepare queries (get, update, percolate, depends of class)
        if(obj instanceof CalendarEvent) {
            // Set completion properties
            ESCalendarEvents esCalendarEvents = new ESCalendarEvents((CalendarEvent) obj);
            esCalendarEvents.name_calendar_suggest.input = Arrays.asList(esCalendarEvents.name.split(" "));

            String updateString = String.format(updateStringTemplate,
                    new Gson().toJson(esCalendarEvents));
            update = new Update.Builder(updateString).index(DEV_CONFERENCES_INDEX)
                    .type(CALENDAREVENTS_TYPE).id(esCalendarEvents.id).build();
            get = new Get.Builder(DEV_CONFERENCES_INDEX, esCalendarEvents.id).type(CALENDAREVENTS_TYPE).build();
            percolate = new Percolate.Builder(DEV_CONFERENCES_INDEX, CALENDAREVENTS_TYPE, updateString).build();
        } else if(obj instanceof Event) {
            // Set completion properties
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

        // Check if the document is still available (!obj.hidden || getResult.found)
        // If not, return.
        if(!objectCanBeUpdated(obj, documentResultGet)) {
            return false;
        }

        DocumentResult documentResultUpdate = client.execute(update);

        // Check the status of Update execution (unchanged, created, updated)
        boolean founded = documentResultGet.getJsonObject().get("found").getAsBoolean();
        Integer oldVersion = null;
        if(founded) {
            oldVersion = documentResultGet.getJsonObject().get("_version").getAsInt();
        }
        Integer newVersion = documentResultUpdate.getJsonObject().get("_version").getAsInt();

        // Begins to build notification message
        NotificationText.Action action;
        if(!founded) {
            action = NotificationText.Action.CREATION;
        } else if(!oldVersion.equals(newVersion)) {
            action = NotificationText.Action.UPDATE;
        } else {
            // neither update nor creation => no more actions
            return false;
        }

        // Find percolators and theirs owners
        JestResult jestResult = client.execute(percolate);

        List<String> matchedPercolators = getMatchesPercolators(jestResult);
        Map<String, UsersRepository.FavouriteItem.FavouriteType> ownersPercolators = getPercolatorsOwners(matchedPercolators);
        List<User> users = usersRepository.getUsers(ownersPercolators);
        final User.Message message;
        if(users.size() > 0) {
            message = users.get(0).new Message();
        } else {
            // No user to notify => no more action
            return true;
        }

        // Notify concerned users
        users.forEach(user -> {
            setupMessage(message, obj, action, ownersPercolators.get(user.name()), (this.updateCounter)++);
            usersRepository.addMessage(user, message);
        });

        return true;
    }

    // Build notification for each user
    private void setupMessage(User.Message message, Object obj, NotificationText.Action action, UsersRepository.FavouriteItem.FavouriteType type, int count) {
        message.date = System.currentTimeMillis();
        message.id = message.date + "." + count;

        NotificationText.What objType = null;
        NotificationText.Why favType = null;
        String objName;

        // What : The begin of the text
        if(obj instanceof Event) {
            Event event = (Event) obj;
            switch (event.type) {
                case CONFERENCE:
                    objType = NotificationText.What.CONFERENCE;
                    break;
                case COMMUNITY:
                    objType = NotificationText.What.COMMUNITY;
                    break;
            }
            objName = event.name;
            message.link = "/event/" + event.id;
        } else if(obj instanceof CalendarEvent) {
            CalendarEvent calendarEvent = (CalendarEvent) obj;
            objType = NotificationText.What.CALENDAR;
            objName = calendarEvent.name;
            message.link = "/calendar/" + calendarEvent.id;
        } else {
            throw new RuntimeException("Unknown class : " + obj.getClass().getName());
        }

        // Why : the 2nd part
        switch (type) {
            case CITY:
                favType = NotificationText.Why.CITY;
                break;
            case TAG:
                favType = NotificationText.Why.SEARCH;
                break;
            case CONFERENCE:
            case COMMUNITY:
            case CALENDAR:
                favType = NotificationText.Why.FAVOURITE;
                break;
        }

        // Action : the 3rd part, is in parameter

        message.text = String.format("%s %s %s : %s", objType.getText(), favType.getText(objType.isFeminine()),
                action.getText(objType.isFeminine()), objName);
    }

    // Extract ids percolators from Percolate execution
    private List<String> getMatchesPercolators(JestResult jestResult) {
        List<String> result = new ArrayList<>();
        jestResult.getJsonObject().get("matches").getAsJsonArray().forEach(jsonElement ->
                result.add(jsonElement.getAsJsonObject().get("_id").getAsString()));

        return result;
    }

    // Extract owners (begin of the id) tfrom the percolaotrs id's list
    private Map<String, UsersRepository.FavouriteItem.FavouriteType> getPercolatorsOwners(List<String> percolatorsIds) {
        Map<String, UsersRepository.FavouriteItem.FavouriteType> owners = new HashMap<>();

        percolatorsIds.forEach(id -> {
            // Structure of ID : <owner>_<type>_<value>
            String[] values = id.split("_", 3);
            UsersRepository.FavouriteItem.FavouriteType favType = UsersRepository.FavouriteItem.FavouriteType.valueOf(values[1]);
            if(!owners.containsKey(values[0])) {
                owners.put(values[0], favType);
            } else {
                // Manage 2+ percolators for an user, with a priority system :
                // TAG < CITY < CONFERENCE, COMMUNITY, CALENDAR (and there are declared in this order,
                // so compare is possible with native function from Enum class)
                if (favType.compareTo(owners.get(values[0])) > 0) {
                    owners.replace(values[0], favType);
                }
            }
        });

        return owners;
    }

    private boolean objectCanBeUpdated(Object obj, DocumentResult getResult) {
        Boolean hidden = null;
        if (obj instanceof CalendarEvent) {
            hidden = ((CalendarEvent) obj).hidden;
        } else if (obj instanceof Event) {
            hidden = ((Event) obj).hidden;
        }

        boolean founded = getResult.getJsonObject().get("found").getAsBoolean();

        // obj won't be updated if he is hidden and not founded (avoid creation of it)
        return (hidden == null || !hidden || founded);


    }

    // ******************************* Event ******************************* //

    public void createEvent(Event event) {
        if (getEvent(event.id) != null) {
            throw new RuntimeException("Event already exists with same id");
        } else {
            Index index = new Index.Builder(event).index(DEV_CONFERENCES_INDEX).type(EVENTS_TYPE)
                    .id(event.id).build();

            client.execute(index);
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

    public void deleteEvent(String eventId) {
        Preconditions.checkNotNull(eventId, "Should not be null !");
        Preconditions.checkArgument(!eventId.equals(""));

        Delete delete = new Delete.Builder(eventId).index(DEV_CONFERENCES_INDEX).type(EVENTS_TYPE).build();

        client.execute(delete);
    }

    // *************************** CalendarEvent *************************** //

    public CalendarEvent getCalendarEvent(String eventId) {
        Get get = new Get.Builder(DEV_CONFERENCES_INDEX, eventId).type(CALENDAREVENTS_TYPE).build();

        JestResult result = client.execute(get);
        if(!result.isSucceeded()) {
            if(result.getResponseCode() == 404) { // Not found : that's not an error
                return null;
            } else {
                throw new RuntimeException(result.getErrorMessage());
            }
        }
        return result.getSourceAsObject(CalendarEvent.class);
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

    // ***************************** Suggests ***************************** //

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

        // Suggests result
        CompletionSearch result = new CompletionSearch();
        result.query = query;
        result.hits = new ArrayList<>();

        // Merge results from all sub-queries in one list (firstly a Map<id,score>, then an List)
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

        // Sort hits : (high score, alphabetical text)
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

    // Add in Map the suggest
    // Manage also already contains keys : scores are in this case added
    private void getSuggestConsumer(SuggestData suggest, HashMap<String, Double> rating) {
        if(rating.containsKey(suggest.text)) {
            rating.replace(suggest.text, suggest.score + rating.get(suggest.text));
        } else {
            rating.put(suggest.text, suggest.score);
        }
    }

    // ******************************* City ******************************* //

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

        // Manage aggreggations
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation cities = aggregations.getAggregation("cities", TermsAggregation.class);

        HashMap<String,CityLight> resultMap = new HashMap<>();

        // Because we use sub-aggregation here (CONFERENCES and COMMUNITIES are in the same type : events),
        // we need a nested for loop
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
                // If CityLight was not created with conferences or communities, then create it
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

        // Search conferences and communities
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

        // Search upcoming events (if the city's location if defined)
        city.location = GeopointCities.getInstance().getLocation(city.name);
        if(city.location != null) {
            city.upcoming_events = findCalendarEventsAround(query, city.location.lat(), city.location.lon(), GEO_DISTANCE);
        } else {
            city.upcoming_events = new ArrayList<>();
        }

        return city;
    }

    // Add event in the good list of city, according to its type
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

    // ***************************** GeoSearch ***************************** //

    public List<Event> findEventsAround(double lat, double lon, double distance) {
        String eventLocations = new SearchSourceBuilder()
                .query(filteredQuery(matchAllQuery(),
                        geoDistanceFilter("gps")
                        .distance(distance, KILOMETERS)
                        .lat(lat).lon(lon)
                ))
                .size(ElasticUtils.MAX_SIZE) // Default max value, or ES will throw an Exception
                .toString();

        Search search = new Search.Builder(eventLocations)
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(EVENTS_TYPE)
                .build();

        SearchResult result = client.execute(search);
        if(!result.isSucceeded()) {
            throw new RuntimeException(result.getErrorMessage());
        }

        return getHitsFromSearch(result, Event.class);
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

    // ****************************** Search ****************************** //

    public EventSearch searchEvents(String query, String page, String limit) {
        return (EventSearch) search(query, page, EVENTS_TYPE, null, null, limit);
    }

    public CalendarEventSearch searchCalendarEvents(String query, String page, String limit) {
        FilterBuilder filterOldCE = rangeFilter("date").gt(System.currentTimeMillis());
        SortBuilder sortByDate = SortBuilders.fieldSort("date").order(SortOrder.ASC);
        return (CalendarEventSearch) search(query, page, CALENDAREVENTS_TYPE, sortByDate, filterOldCE, limit);
    }

    // Generic function of search
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
        QueryBuilder queryBuilder = getQueryBuilder(query.toLowerCase());

        if (filter == null) {
            searchQuery.query(queryBuilder);
        } else {
            searchQuery.query(filteredQuery(queryBuilder, filter));
        }

        Count count = new Count.Builder().query(searchQuery.toString())
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(typeSearch)
                .build();

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

    public SearchResult searchByIds(String type, List<String> ids) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        IdsQueryBuilder idsQueryFilter = new IdsQueryBuilder();

        ids.forEach(idsQueryFilter::addIds);

        searchSourceBuilder.query(idsQueryFilter).size(ElasticUtils.MAX_SIZE);

        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(DEV_CONFERENCES_INDEX).addType(type).build();

        return client.execute(search);
    }

    // *************************** Miscellaneous *************************** //

    // Extract List with casted hits from a SearchResult
    public static List getHitsFromSearch(SearchResult searchResult, Class<?> sourceType) {
        return (searchResult.getHits(sourceType).stream()
                .map((data) -> data.source).collect(Collectors.toList())
        );
    }

    // Choose between matchAllQuery and queryStringQuery depending of query
    private QueryBuilder getQueryBuilder(String query) {
        if(query == null || query.equals("") || query.equals("undefined")) {
            return QueryBuilders.boolQuery()
                    .must(matchAllQuery());
        } else {
            /* Boost system : id >>> name >>> tags > description, url
             *
             * NB : "tags" helps in boost, however a document with only
             *      the tag won't be shown with this search query)
             */
            return QueryBuilders.boolQuery()
                    .must(QueryBuilders.queryStringQuery(QueryParser.escape(query))
                            .field("id", 6).field("name", 3).field("description").field("url")
                            .boost(2))
                    .should(queryStringQuery(QueryParser.escape(query)).field("name"))
                    .should(termQuery("tags", query));
        }
    }
}
