package org.devconferences.elastic;

import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;

import java.io.IOException;
import java.util.Set;

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
    public <T extends JestResult> void executeAsync(Action<T> clientRequest, JestResultHandler<T> jestResultHandler) {
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
}
