package org.devconferences.events;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import io.searchbox.client.JestResult;
import io.searchbox.core.*;
import io.searchbox.core.search.aggregation.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.elastic.SuggestData;
import org.devconferences.elastic.SuggestResponse;
import org.devconferences.events.data.*;
import org.devconferences.events.search.CalendarEventSearchResult;
import org.devconferences.events.search.CompletionResult;
import org.devconferences.events.search.EventSearchResult;
import org.devconferences.events.search.PaginatedSearchResult;
import org.devconferences.users.NotificationText;
import org.devconferences.users.User;
import org.devconferences.users.UsersRepository;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
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

import static org.devconferences.elastic.ElasticUtils.*;
import static org.elasticsearch.common.unit.DistanceUnit.KILOMETERS;
import static org.elasticsearch.index.query.FilterBuilders.geoDistanceFilter;
import static org.elasticsearch.index.query.FilterBuilders.rangeFilter;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

@Singleton
public class EventsRepository {
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
        Class<?> clazz;

        // Prepare queries (get, update, percolate, depends of class)
        if(obj instanceof AbstractEvent) {
            AbstractEvent abstractEvent = (AbstractEvent) obj;
            AbstractEvent abstractEventInES;
            String type;
            ArrayList<String> suggestList = new ArrayList<>();

            suggestList.addAll(Arrays.stream(abstractEvent.name.split("[\\s,]")).distinct()
                    .filter((value) -> !value.matches("[-:]")).collect(Collectors.toList()));
            suggestList.addAll(abstractEvent.tags);
            if(obj instanceof CalendarEvent) {
                type = CALENDAREVENTS_TYPE;
                clazz = CalendarEvent.class;
                abstractEventInES = new ESCalendarEvents((CalendarEvent) obj);
                ((ESCalendarEvents) abstractEventInES).suggests.input.addAll(suggestList);
            } else if(obj instanceof Event) {
                type = EVENTS_TYPE;
                clazz = Event.class;
                abstractEventInES = new ESEvents((Event) obj);
                suggestList.add(((Event) obj).city);
                ((ESEvents) abstractEventInES).suggests.input.addAll(suggestList);
            } else {
                throw new IllegalStateException("Unknown class : " + obj.getClass());
            }

            String updateString = String.format(updateStringTemplate,
                    new Gson().toJson(abstractEventInES));
            update = new Update.Builder(updateString).index(DEV_CONFERENCES_INDEX)
                    .type(type).id(abstractEvent.id).build();
            get = new Get.Builder(DEV_CONFERENCES_INDEX, abstractEvent.id).type(type).build();
            percolate = new Percolate.Builder(DEV_CONFERENCES_INDEX, type, updateString).build();
        } else {
            throw new IllegalStateException("Unknown class : " + obj.getClass());
        }

        // Execute queries
        DocumentResult documentResultGet = client.execute(get);

        // Check if the document is still available (!obj.hidden || getResult.found)
        // If not, return.
        if(!checkHiddenStatus(obj, documentResultGet)) {
            return false;
        }

        // Check if obj will create, update, or do nothing when use Update
        boolean founded = documentResultGet.getJsonObject().get("found").getAsBoolean();
        Object sourceOfGetResult = documentResultGet.getSourceAsObject(clazz);
        NotificationText.Action action;

        if(!founded) {
            action = NotificationText.Action.CREATION;
        } else {
            // Check if sourceOfGetResult and obj are equals (NOT WITH ES* CLASSES, BUT WITH THEIR SUPER CLASSES,
            // their specific properties change must not implies a notification,
            // that's an hidden property from outside)
            if(!clazz.cast(obj).equals(sourceOfGetResult)) {
                action = NotificationText.Action.UPDATE;
            } else {
                // neither update nor creation => no more actions
                return false;
            }
        }

        client.execute(update);

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
            switch(event.type) {
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
            throw new IllegalStateException("Unknown class : " + obj.getClass());
        }

        // Why : the 2nd part
        switch(type) {
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
        message.text = String.format("%s : %s", new NotificationText(objType, favType, action), objName);
    }

    // Extract ids percolators from Percolate execution
    private List<String> getMatchesPercolators(JestResult jestResult) {
        List<String> result = new ArrayList<>();
        jestResult.getJsonObject().get("matches").getAsJsonArray().forEach(jsonElement ->
                result.add(jsonElement.getAsJsonObject().get("_id").getAsString()));

        return result;
    }

    // Extract owners (begin of the id) from the percolators id's list
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
                if(favType.compareTo(owners.get(values[0])) > 0) {
                    owners.replace(values[0], favType);
                }
            }
        });

        return owners;
    }

    private boolean checkHiddenStatus(Object obj, DocumentResult getResult) {
        Boolean hidden;
        if(obj instanceof AbstractEvent) {
            hidden = ((AbstractEvent) obj).hidden;
        } else {
            throw new IllegalStateException("unknown class : " + obj.getClass());
        }

        boolean founded = getResult.getJsonObject().get("found").getAsBoolean();

        // obj won't be updated if he is hidden and not founded (avoid creation of it)
        return (hidden == null || !hidden || founded);


    }

    // ******************************* Event ******************************* //

    void createEvent(Event event) {
        if(getEvent(event.id) != null) {
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

    void deleteEvent(String eventId) {
        Preconditions.checkNotNull(eventId, "Should not be null !");
        Preconditions.checkArgument(!eventId.equals(""));

        Delete delete = new Delete.Builder(eventId).index(DEV_CONFERENCES_INDEX).type(EVENTS_TYPE).build();

        client.execute(delete);
    }

    // *************************** CalendarEvent *************************** //

    CalendarEvent getCalendarEvent(String eventId) {
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

    List<CalendarEvent> getCalendarEventList(String page) {
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

        return new CalendarEventSearchResult().getHitsFromSearch(searchResult);
    }

    // ***************************** Suggests ***************************** //

    CompletionResult suggest(String query, User user) {
        SuggestBuilder suggestBuilder = new SuggestBuilder();
        suggestBuilder.addSuggestion(
                SuggestBuilders.completionSuggestion("suggests")
                        .field("suggests").text(query)
        );

        JestResult jestResult = new JestResult(new Gson());
        try {
            Suggest suggest = new Suggest.Builder(XContentHelper.convertToJson(suggestBuilder.buildAsBytes(), false))
                    .addIndex(DEV_CONFERENCES_INDEX).build();

            jestResult = client.execute(suggest);
        } catch(IOException e) {
            e.printStackTrace();
        }

        // Suggests result
        CompletionResult result = new CompletionResult();
        result.query = query;

        // Merge results from all sub-queries in one list (firstly a Map<id,score>, then an List)
        HashMap<String, Double> rating = new HashMap<>();
        SuggestResponse test = new Gson().fromJson(jestResult.getJsonObject(), SuggestResponse.class);

        test.suggests.get(0).options.forEach((suggest) -> getSuggestConsumer(suggest, rating));

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
            if(suggO != null && suggT1 != null) {
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

    List<CityLight> getAllCitiesWithQuery(String query) {
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

        // Manage aggregations
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation cities = aggregations.getAggregation("cities", TermsAggregation.class);

        HashMap<String, CityLight> resultMap = new HashMap<>();

        // Because we use sub-aggregation here (CONFERENCES and COMMUNITIES are in the same type : events),
        // we need a nested for loop
        for(TermsAggregation.Entry city : cities.getBuckets()) {
            TermsAggregation types = city.getTermsAggregation("types");
            CityLight cityLight = new CityLight(city.getKey(), city.getKey());
            cityLight.location = GeopointCities.getInstance().getLocation(city.getKey());
            for(TermsAggregation.Entry type : types.getBuckets()) {
                switch(type.getKey()) {
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
        FilterAggregation filterAggregation = getAllCalendarEventCountByCity(query);

        // Parse result
        GeopointCities.getInstance().getAllLocations().forEach((cityName, location) -> {
            // Get corresponding aggregation
            int countForCity = getCountForACity(filterAggregation, cityName);
            if(countForCity > 0) {
                // If CityLight was not created with conferences or communities, then create it
                if(!resultMap.containsKey(cityName)) {
                    CityLight cityLight = new CityLight(cityName, cityName);
                    cityLight.location = location;
                    resultMap.put(cityName, cityLight);
                }
                resultMap.get(cityName).totalCalendar += countForCity;
            }
        });

        // HashMap -> List
        return resultMap.values().stream().map((city) -> {
            city.count = city.totalCalendar + city.totalCommunity + city.totalConference;
            return city;
        }).sorted().collect(Collectors.toList());
    }

    private FilterAggregation getAllCalendarEventCountByCity(String query) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // Each location will be a sub-aggregation : it groups n count queries into 1.
        FilterAggregationBuilder filterAggregationBuilder = AggregationBuilders.filter("all")
                .filter(FilterBuilders.queryFilter(getQueryBuilder(query)));
        searchSourceBuilder.aggregation(filterAggregationBuilder);

        // Build aggregation for each location
        GeopointCities.getInstance().getAllLocations().forEach((key, value) -> {
            filterAggregationBuilder.subAggregation(AggregationBuilders.geoDistance(key)
                    .lon(value.lon()).lat(value.lat()).unit(KILOMETERS)
                    .addUnboundedTo(GEO_DISTANCE).field("location.gps"));
        });

        Search geoSearchAggs = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(CALENDAREVENTS_TYPE)
                .build();
        SearchResult calendarAggResult = client.execute(geoSearchAggs);

        return calendarAggResult.getAggregations().getFilterAggregation("all");
    }

    City getCity(String cityId, String query) {
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
        switch(event.type) {
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

    // Extract count of city key from the FilterAggregation of getAllCitiesWithQuery()
    private int getCountForACity(FilterAggregation filterAggregation, String city) {
        return filterAggregation.getGeoDistanceAggregation(city).getBuckets().get(0).getCount().intValue();
    }

    // ***************************** GeoSearch ***************************** //

    List<Event> findEventsAround(double lat, double lon, double distance) {
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

        return new EventSearchResult().getHitsFromSearch(result);
    }

    List<CalendarEvent> findCalendarEventsAround(String query, double lat, double lon, double distance) {
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
        return new CalendarEventSearchResult().getHitsFromSearch(result);
    }

    // ****************************** Search ****************************** //

    EventSearchResult searchEvents(String query, Integer page, Integer limit) {
        return (EventSearchResult) search(query, page, EVENTS_TYPE, null, null, limit);
    }

    CalendarEventSearchResult searchCalendarEvents(String query, Integer page, Integer limit) {
        FilterBuilder filterOldCE = rangeFilter("date").gt(System.currentTimeMillis());
        SortBuilder sortByDate = SortBuilders.fieldSort("date").order(SortOrder.ASC);
        return (CalendarEventSearchResult) search(query, page, CALENDAREVENTS_TYPE, sortByDate, filterOldCE, limit);
    }

    // Generic function of search
    private PaginatedSearchResult search(String query, Integer page, String typeSearch, SortBuilder sortBy, FilterBuilder filter, Integer limit) {
        SearchSourceBuilder searchQuery = new SearchSourceBuilder();
        final int pageInt = (page == null ? 1 : page);
        final int perPage = (limit == null ? 10 : limit);

        // Check parameters
        if(pageInt <= 0) {
            throw new RuntimeException("HTML 400 : page parameter must be positive");
        }
        if(query == null || query.equals("undefined")) {
            throw new RuntimeException("HTML 400 : query parameter is missing");
        }
        if(perPage < 1 || perPage > 1000) {
            throw new RuntimeException("HTML 400 : limit parameter must be between 1 and 1000");
        }

        // Count query
        QueryBuilder queryBuilder = getQueryBuilder(query.toLowerCase());

        if(filter == null) {
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
        if(perPage * (pageInt - 1) >= countResult.getCount() && (countResult.getCount() != 0 || pageInt != 1)) {
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
                res = new EventSearchResult();
                ((EventSearchResult) res).hits.addAll(((EventSearchResult) res).getHitsFromSearch(searchResult));
                break;
            case CALENDAREVENTS_TYPE:
                res = new CalendarEventSearchResult();
                ((CalendarEventSearchResult) res).hits.addAll(((CalendarEventSearchResult) res).getHitsFromSearch(searchResult));
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

    // Choose between matchAllQuery and queryStringQuery depending of query
    public QueryBuilder getQueryBuilder(String query) {
        if(query == null || query.equals("") || query.equals("undefined")) {
            return QueryBuilders.boolQuery()
                    .must(matchAllQuery());
        } else {
            // Boost system : id >>> name >>> tags > description, url
            return QueryBuilders.boolQuery()
                    .must(QueryBuilders.queryStringQuery(QueryParser.escape(query)).defaultOperator(QueryStringQueryBuilder.Operator.AND)
                            .field("id", 10).field("name", 5).field("tags", 2).field("description").field("url"));
        }
    }
}
