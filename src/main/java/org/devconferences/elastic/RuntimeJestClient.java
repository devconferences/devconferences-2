package org.devconferences.elastic;

import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.CountResult;
import io.searchbox.core.SearchResult;
import org.devconferences.events.Event;

/**
 * Created by chris on 08/06/15.
 */
public interface RuntimeJestClient extends JestClient, AutoCloseable {
    <T extends JestResult> T execute(Action<T> clientRequest);

    @Override
    void close();

    void indexES(String type, Object event, String id);
    SearchResult searchES(String type, String query);
    CountResult countES(String type, String query);
    JestResult getES(String type, String id);
    void deleteES(String type, String id);
}
