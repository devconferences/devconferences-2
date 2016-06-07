package org.devconferences.users;

import com.google.inject.Singleton;
import io.searchbox.client.JestResult;
import io.searchbox.core.Get;
import io.searchbox.core.Index;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.elastic.RuntimeJestClientAdapter;

import static org.devconferences.elastic.ElasticUtils.DEV_CONFERENCES_INDEX;
import static org.devconferences.elastic.ElasticUtils.createClient;

/**
 * Created by chris on 07/06/15.
 */
@Singleton
public class UsersRepository {
    public static final String USERS_TYPE = "users";

    private final RuntimeJestClient client;

    public UsersRepository() {
        this(ElasticUtils.createClient());
    }

    public UsersRepository(RuntimeJestClient client) {
        this.client = client;
    }

    public void save(User user) {
        client.indexES(USERS_TYPE, user, user.login);
    }

    public User getUser(String userId) {
        JestResult jestResult = client.getES(USERS_TYPE, userId);
        return jestResult.getSourceAsObject(User.class);
    }
}
