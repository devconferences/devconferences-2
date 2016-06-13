package org.devconferences.users;

import com.google.gson.Gson;
import com.google.inject.Singleton;
import io.searchbox.client.JestResult;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Get;
import io.searchbox.core.Index;
import io.searchbox.core.Update;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.devconferences.elastic.ElasticUtils.DEV_CONFERENCES_INDEX;
import static org.devconferences.elastic.ElasticUtils.createClient;

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

    public UsersRepository() {
        this(ElasticUtils.createClient());
    }

    public UsersRepository(RuntimeJestClient client) {
        this.client = client;
    }

    public void save(User user) {
        Index index = new Index.Builder(user).index(DEV_CONFERENCES_INDEX)
                .type(USERS_TYPE).id(user.login).build();

        client.execute(index);
    }

    public User getUser(String userId) {
        Get get = new Get.Builder(DEV_CONFERENCES_INDEX, userId).type(USERS_TYPE).build();
        JestResult jestResult = client.execute(get);
        return jestResult.getSourceAsObject(User.class);
    }

    public void addFavourite(User user, FavouriteItem.FavouriteType type, String value) {
        List<String> listItems = getListItems(user, type);

        // Try to add $value in $listItems
        if(!listItems.contains(value)) {
            listItems.add(value);
            Collections.sort(listItems);
        }

        DocumentResult documentResult = updateFavourites(user);
    }

    public void removeFavourite(User user, FavouriteItem.FavouriteType type, String value) {
        List<String> listItems = getListItems(user, type);

        // Try to remove $value from $listItems
        if(listItems.contains(value)) {
            listItems.remove(value);
        }

        DocumentResult documentResult = updateFavourites(user);
    }

    List<String> getListItems(User user, FavouriteItem.FavouriteType type) {
        List<String> listItems;
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
            default:
                listItems = new ArrayList<>();
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
}
