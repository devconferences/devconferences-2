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
        Index index = new Index.Builder(user).index(DEV_CONFERENCES_INDEX)
                .type(USERS_TYPE).id(user.login).build();

        client.execute(index);
    }

    public User getUser(String userId) {
        Get get = new Get.Builder(DEV_CONFERENCES_INDEX, userId).type(USERS_TYPE).build();
        JestResult jestResult = client.execute(get);
        return jestResult.getSourceAsObject(User.class);
    }
}
