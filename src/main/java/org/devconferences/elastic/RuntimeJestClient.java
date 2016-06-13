package org.devconferences.elastic;

import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.CountResult;
import io.searchbox.core.SearchResult;

/**
 * Created by chris on 08/06/15.
 */
public interface RuntimeJestClient extends JestClient, AutoCloseable {
    <T extends JestResult> T execute(Action<T> clientRequest);

    @Override
    void close();
}
