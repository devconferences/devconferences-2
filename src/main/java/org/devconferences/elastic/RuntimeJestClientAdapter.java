package org.devconferences.elastic;

import com.google.gson.Gson;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.*;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.search.suggest.SuggestBuilder;

import java.io.IOException;
import java.util.Set;

import static org.devconferences.elastic.ElasticUtils.DEV_CONFERENCES_INDEX;

/**
 * Created by chris on 08/06/15.
 */
public class RuntimeJestClientAdapter implements RuntimeJestClient {
    private final JestClient jestClient;

    public RuntimeJestClientAdapter(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    @Override
    public <T extends JestResult> T execute(Action<T> clientRequest) {
        try {
            return jestClient.execute(clientRequest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T extends JestResult> void executeAsync(Action<T> clientRequest, JestResultHandler<? super T> jestResultHandler) {
        jestClient.executeAsync(clientRequest, jestResultHandler);
    }

    @Override
    public void shutdownClient() {
        jestClient.shutdownClient();
    }

    @Override
    public void setServers(Set<String> servers) {
        jestClient.setServers(servers);
    }

    @Override
    public void close() {
        shutdownClient();
    }

    @Override
    public int indexES(String type, Object event, String id) {
        Index index = new Index.Builder(event).index(DEV_CONFERENCES_INDEX).type(type).id(id).build();

        JestResult result = execute(index);
        if(!result.isSucceeded()) {
            throw new IllegalStateException("Can't index '" + id + "' in type '" + type + "' : " + result.getErrorMessage());
        }

        return 0;
    }

    @Override
    public SearchResult searchES(String type, String query) {
        Search search = new Search.Builder(query)
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(type)
                .build();

        return execute(search);
    }

    @Override
    @Deprecated
    public CountResult countES(String type, String query) {
        Count count = new Count.Builder()
                .query(query)
                .addIndex(DEV_CONFERENCES_INDEX)
                .addType(type)
                .build();

        return execute(count);
    }

    @Override
    public JestResult getES(String type, String id) {
        Get get = new Get.Builder(DEV_CONFERENCES_INDEX, id).type(type).build();

        return execute(get);
    }

    @Override
    public JestResult suggestES(String query) {
        Suggest suggest = new Suggest.Builder(query).build();

        return execute(suggest);
    }

    @Override
    public int deleteES(String type, String id) {
        Delete delete = new Delete.Builder(id).index(DEV_CONFERENCES_INDEX).type(type).build();

        execute(delete);

        return 0;
    }

    @Override
    public int deleteAllES(String type) {
        ElasticUtils.deleteData(type);

        return 0;
    }
}
