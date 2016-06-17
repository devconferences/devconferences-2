package org.devconferences.users;

import com.google.gson.Gson;
import com.google.inject.Singleton;
import io.searchbox.client.JestResult;
import io.searchbox.core.*;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.events.CalendarEvent;
import org.devconferences.events.Event;
import org.devconferences.events.EventsRepository;
import org.devconferences.events.search.SimpleSearchResult;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.devconferences.elastic.ElasticUtils.DEV_CONFERENCES_INDEX;

/**
 * Created by chris on 07/06/15.
 */
@Singleton
public class UsersRepository {

    public static class FavouriteItem {
        public FavouriteType type;
        public String value;

        public enum FavouriteType {
            CITY, TAG, CONFERENCE, COMMUNITY, CALENDAR
        }
    }
    public static final String USERS_TYPE = "users";

    private final RuntimeJestClient client;
    private final EventsRepository eventsRepository;

    public UsersRepository() {
        this(ElasticUtils.createClient(), new EventsRepository());
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
            Class classType;
            switch (typeEnum) {
                case CONFERENCE:
                    searchResult = eventsRepository.searchByIds(EventsRepository.EVENTS_TYPE, user.favourites.conferences);
                    result = new SimpleSearchResult<Event>();
                    classType = Event.class;
                    break;
                case COMMUNITY:
                    searchResult = eventsRepository.searchByIds(EventsRepository.EVENTS_TYPE, user.favourites.communities);
                    result = new SimpleSearchResult<Event>();
                    classType = Event.class;
                    break;
                case CALENDAR:
                    searchResult = eventsRepository.searchByIds(EventsRepository.CALENDAREVENTS_TYPE, user.favourites.upcomingEvents);
                    result = new SimpleSearchResult<CalendarEvent>();
                    classType = CalendarEvent.class;
                    break;
                default:
                    throw new RuntimeException("HTML 400 : Unsupported FavouriteType : " + typeEnum);
            }
            result.query = "favourites/" + typeEnum.toString();
            result.hits = eventsRepository.getHitsFromSearch(searchResult, classType);

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

    public List<User> getUsers(List<String> userIds) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        IdsQueryBuilder idsQueryBuilder = QueryBuilders.idsQuery();

        userIds.forEach(idsQueryBuilder::addIds);

        searchSourceBuilder.query(idsQueryBuilder).size(ElasticUtils.MAX_SIZE);

        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(DEV_CONFERENCES_INDEX).addType(USERS_TYPE).build();

        SearchResult searchResult = client.execute(search);

        return EventsRepository.getHitsFromSearch(searchResult, User.class);
    }

    public DocumentResult addFavourite(User user, FavouriteItem.FavouriteType type, String value) {
        List<String> listItems = getListItems(user, type);

        // Try to add $value in $listItems
        if(!listItems.contains(value)) {
            listItems.add(value);
            Collections.sort(listItems);

            // Add percolate query
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.queryStringQuery(value));
            if(type == FavouriteItem.FavouriteType.TAG) {
                Index index = new Index.Builder(
                        searchSourceBuilder.toString()
                ).index(DEV_CONFERENCES_INDEX).type(".percolator").id(user.name() + "_" + value).build();

                DocumentResult documentResult = client.execute(index);
                if(!documentResult.isSucceeded()) {
                    throw new RuntimeException("Impossible to create percolator " + user.name() + "_" + value + "\n" +
                            documentResult.getErrorMessage());
                }
            }
        }

        return updateFavourites(user);
    }

    public DocumentResult removeFavourite(User user, FavouriteItem.FavouriteType type, String value) {
        List<String> listItems = getListItems(user, type);

        // Try to remove $value from $listItems
        if(listItems.contains(value)) {
            listItems.remove(value);

            // Remove percolate query
            if(type == FavouriteItem.FavouriteType.TAG) {
                Delete delete = new Delete.Builder(user.name() + "_" + value)
                        .index(DEV_CONFERENCES_INDEX).type(".percolator").id(user.name() + "_" + value).build();

                DocumentResult documentResult = client.execute(delete);
                if(!documentResult.isSucceeded()) {
                    throw new RuntimeException("Impossible to remove percolator " + user.name() + "_" + value + "\n" +
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

    DocumentResult updateFavourites(User user) {
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

    DocumentResult updateMessages(User user) {
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
