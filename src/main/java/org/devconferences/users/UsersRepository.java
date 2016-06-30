package org.devconferences.users;

import com.google.gson.Gson;
import com.google.inject.Singleton;
import io.searchbox.client.JestResult;
import io.searchbox.core.*;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.events.EventsRepository;
import org.devconferences.events.GeopointCities;
import org.devconferences.events.search.CalendarEventSearchResult;
import org.devconferences.events.search.EventSearchResult;
import org.devconferences.events.search.SimpleSearchResult;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.devconferences.elastic.ElasticUtils.DEV_CONFERENCES_INDEX;
import static org.devconferences.elastic.ElasticUtils.createClient;
import static org.devconferences.users.UsersRepository.FavouriteItem.FavouriteType.CITY;
import static org.elasticsearch.common.unit.DistanceUnit.KILOMETERS;
import static org.elasticsearch.index.query.FilterBuilders.geoDistanceFilter;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

@Singleton
public class UsersRepository {

    public static class FavouriteItem {
        public FavouriteType type;
        public String value;

        public enum FavouriteType {
            TAG, CITY, CONFERENCE, COMMUNITY, CALENDAR
        }
    }

    public static final String USERS_TYPE = "users";

    private final RuntimeJestClient client;
    private final EventsRepository eventsRepository;

    public UsersRepository() {
        this.client = createClient();
        this.eventsRepository = new EventsRepository(createClient(), this);
    }

    public UsersRepository(RuntimeJestClient client, EventsRepository eventsRepository) {
        this.client = client;
        this.eventsRepository = eventsRepository;
    }

    public void save(User user) {
        Index index = new Index.Builder(user).index(DEV_CONFERENCES_INDEX)
                .type(USERS_TYPE).id(user.login).build();

        client.execute(index);
    }

    public SimpleSearchResult getFavourites(User user, FavouriteItem.FavouriteType typeEnum) {
        if(user != null) {
            SearchResult searchResult;
            SimpleSearchResult result;
            switch(typeEnum) {
                case CONFERENCE:
                    searchResult = eventsRepository.searchByIds(EventsRepository.EVENTS_TYPE, user.favourites.conferences);
                    result = new EventSearchResult();
                    ((EventSearchResult) result).hits.addAll(((EventSearchResult) result).getHitsFromSearch(searchResult));
                    break;
                case COMMUNITY:
                    searchResult = eventsRepository.searchByIds(EventsRepository.EVENTS_TYPE, user.favourites.communities);
                    result = new EventSearchResult();
                    ((EventSearchResult) result).hits.addAll(((EventSearchResult) result).getHitsFromSearch(searchResult));
                    break;
                case CALENDAR:
                    searchResult = eventsRepository.searchByIds(EventsRepository.CALENDAREVENTS_TYPE, user.favourites.upcomingEvents);
                    result = new CalendarEventSearchResult();
                    ((CalendarEventSearchResult) result).hits.addAll(((CalendarEventSearchResult) result).getHitsFromSearch(searchResult));
                    break;
                default:
                    throw new RuntimeException("HTML 400 : Unsupported FavouriteType : " + typeEnum);
            }
            result.query = "favourites/" + typeEnum.toString();

            return result;
        } else {
            return null;
        }
    }

    public User getUser(String userId) {
        Get get = new Get.Builder(DEV_CONFERENCES_INDEX, userId).type(USERS_TYPE).build();
        JestResult jestResult = client.execute(get);
        return jestResult.getSourceAsObject(User.class);
    }

    public List<User> getUsers(Map<String, FavouriteItem.FavouriteType> userIds) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        IdsQueryBuilder idsQueryBuilder = QueryBuilders.idsQuery();

        userIds.keySet().forEach(idsQueryBuilder::addIds);

        searchSourceBuilder.query(idsQueryBuilder).size(ElasticUtils.MAX_SIZE);

        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(DEV_CONFERENCES_INDEX).addType(USERS_TYPE).build();

        SearchResult searchResult = client.execute(search);

        return new SimpleSearchResult<User>().getHitsFromSearch(searchResult, User.class);
    }

    public DocumentResult addFavourite(User user, FavouriteItem.FavouriteType type, String value) {
        List<String> listItems = getListItems(user, type);

        // Try to add $value in $listItems
        if(!listItems.contains(value)) {
            listItems.add(value);
            Collections.sort(listItems);

            // Add percolate query
            List<Index> indexes = new ArrayList<>();
            String percolatorId = user.name() + "_" + type.name() + "_" + value;
            switch(type) {
                case CITY:
                    // 2 searches : conference/commu with city term + calendar with geosearch (if gps of city is defined)
                    GeoPoint geoPoint = GeopointCities.getInstance().getLocation(value);
                    indexes.add(new Index.Builder(
                            new SearchSourceBuilder().query(QueryBuilders.termQuery("city", value)).toString()
                    ).index(DEV_CONFERENCES_INDEX).type(".percolator").id(percolatorId).build());
                    if(geoPoint != null) {
                        indexes.add(
                                new Index.Builder(new SearchSourceBuilder()
                                        .query(filteredQuery(matchAllQuery(),
                                                geoDistanceFilter("location.gps")
                                                        .distance(20d, KILOMETERS)
                                                        .lat(geoPoint.lat()).lon(geoPoint.lon()))).toString()
                                ).index(DEV_CONFERENCES_INDEX).type(".percolator").id(percolatorId + "_geo").build());
                    }
                    break;
                case TAG:
                    indexes.add(new Index.Builder(
                            new SearchSourceBuilder().query(QueryBuilders.queryStringQuery(value).defaultOperator(QueryStringQueryBuilder.Operator.AND)).toString()
                    ).index(DEV_CONFERENCES_INDEX).type(".percolator").id(percolatorId).build());
                    break;
                case CONFERENCE:
                case COMMUNITY:
                case CALENDAR:
                    indexes.add(new Index.Builder(
                            new SearchSourceBuilder().query(QueryBuilders.termQuery("id", value)).toString()
                    ).index(DEV_CONFERENCES_INDEX).type(".percolator").id(percolatorId).build());
                    break;
            }
            indexes.forEach(index -> {
                DocumentResult documentResult = client.execute(index);
                if(!documentResult.isSucceeded()) {
                    throw new RuntimeException("Impossible to create percolator " + index.getId() + "\n" +
                            documentResult.getErrorMessage());
                }
            });
        }

        return updateFavourites(user);
    }

    public DocumentResult removeFavourite(User user, FavouriteItem.FavouriteType type, String value) {
        List<String> listItems = getListItems(user, type);

        // Try to remove $value from $listItems
        if(listItems.contains(value)) {
            listItems.remove(value);

            // Remove percolate query
            String percolatorId = user.name() + "_" + type.name() + "_" + value;
            Delete delete = new Delete.Builder(percolatorId)
                    .index(DEV_CONFERENCES_INDEX).type(".percolator").build();

            DocumentResult documentResult = client.execute(delete);
            if(!documentResult.isSucceeded()) {
                throw new RuntimeException("Impossible to remove percolator " + percolatorId + "\n" +
                        documentResult.getErrorMessage());
            }
            // Remove also geosearch if type is CITY
            if(type == CITY) {
                delete = new Delete.Builder(percolatorId + "_geo")
                        .index(DEV_CONFERENCES_INDEX).type(".percolator").build();

                documentResult = client.execute(delete);
                if(!documentResult.isSucceeded()) {
                    throw new RuntimeException("Impossible to remove percolator " + percolatorId + "_geo" + "\n" +
                            documentResult.getErrorMessage());
                }
            }
        }

        return updateFavourites(user);
    }

    List<String> getListItems(User user, FavouriteItem.FavouriteType type) {
        List<String> listItems = new ArrayList<>();
        switch(type) {
            case CITY:
                listItems = user.favourites.cities;
                break;
            case TAG:
                listItems = user.favourites.tags;
                break;
            case CONFERENCE:
                listItems = user.favourites.conferences;
                break;
            case COMMUNITY:
                listItems = user.favourites.communities;
                break;
            case CALENDAR:
                listItems = user.favourites.upcomingEvents;
                break;
        }
        return listItems;
    }

    private DocumentResult updateFavourites(User user) {
        String updateStringTemplate = "{" +
                "  \"doc\": {" +
                "    \"favourites\": %s" +
                "  }" +
                "}";

        String updateString = String.format(updateStringTemplate, new Gson().toJson(user.favourites));

        Update update = new Update.Builder(updateString)
                .index(DEV_CONFERENCES_INDEX).type(USERS_TYPE).id(user.login).build();

        return client.execute(update);

    }

    public DocumentResult addMessage(User user, User.Message message) {
        user.messages.add(message);

        return updateMessages(user);
    }

    public DocumentResult deleteMessage(User user, String id) {
        // Remove messages with matches id
        user.messages.stream().filter(message -> message.id.equals(id))
                .collect(Collectors.toList())
                .forEach(user.messages::remove);

        return updateMessages(user);
    }

    private DocumentResult updateMessages(User user) {
        String updateStringTemplate = "{" +
                "  \"doc\": {" +
                "    \"messages\": %s" +
                "  }" +
                "}";
        String updateString = String.format(updateStringTemplate, new Gson().toJson(user.messages));

        Update update = new Update.Builder(updateString)
                .index(DEV_CONFERENCES_INDEX).type(USERS_TYPE).id(user.login).build();

        return client.execute(update);
    }
}
