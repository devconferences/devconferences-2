package org.devconferences.users;

import com.google.inject.Singleton;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Get;
import io.searchbox.core.Index;
import org.devconferences.users.User;

import java.io.IOException;

import static org.devconferences.elastic.Elastic.DEV_CONFERENCES_INDEX;
import static org.devconferences.elastic.Elastic.createClient;

/**
 * Created by chris on 07/06/15.
 */
@Singleton
public class UsersRepository {
    public static final String USERS_TYPE = "users";

    private final JestClient client;

    public UsersRepository() {
        client = createClient();
    }

    public void save(User user) {
        Index index = new Index.Builder(user).index(DEV_CONFERENCES_INDEX).type(USERS_TYPE).id(user.login).build();
        try {
            client.execute(index);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public User getUser(String userId) {
        Get get = new Get.Builder(DEV_CONFERENCES_INDEX, userId).type(USERS_TYPE).build();

        try {
            JestResult jestResult = client.execute(get);
            return jestResult.getSourceAsObject(User.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
