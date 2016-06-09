package org.devconferences.elastic;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import io.searchbox.client.JestResult;
import io.searchbox.core.CountResult;
import io.searchbox.core.SearchResult;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Create and configure mocks of RuntimeJestClientAdapter for tests.
 */
public class MockJestClient {
    // Explanation of this in test of Jest-client, e.g :
    // https://github.com/searchbox-io/Jest/blob/master/jest-common/src/test/java/io/searchbox/core/SearchResultTest.java
    private static final String countStringTemplate = "" +
            "{" +
            "    \"count\" : %s," +
            "    \"_shards\" : {" +
            "        \"total\" : 5," +
            "        \"successful\" : 5," +
            "        \"failed\" : 0" +
            "    }" +
            "}"
            ;
    private static final String searchStringTemplate = "" +
            "{" +
            "    \"_shards\":{" +
            "        \"total\" : 5," +
            "        \"successful\" : 5," +
            "        \"failed\" : 0" +
            "    }," +
            "    \"hits\":{" +
            "        \"total\" : %s," +
            "        \"max_score\": 0.0," +
            "        \"hits\" : %s" +
            "    }," +
            "    \"aggregations\" : %s" +
            "}";
    public static RuntimeJestClientAdapter createMock(String typeES) {
        RuntimeJestClientAdapter result = mock(RuntimeJestClientAdapter.class);

        when(result.indexES(eq(typeES), anyObject(), anyString())).thenReturn(0);
        when(result.deleteES(eq(typeES), anyString())).thenReturn(0);
        return result;
    }

    public static void configCount(RuntimeJestClientAdapter mock, String typeES, int count) {
        String jsonCount = String.format(countStringTemplate, count);
        CountResult mockCountResult = new CountResult(new Gson());
        mockCountResult.setSucceeded(true);
        mockCountResult.setJsonString(jsonCount);
        mockCountResult.setJsonObject(new JsonParser().parse(jsonCount).getAsJsonObject());
        mockCountResult.setPathToResult("count");

        when(mock.countES(eq(typeES), anyString())).thenReturn(mockCountResult);

    }

    public static void configSearch(RuntimeJestClientAdapter mock, String typeES, int count, String hits, String aggregations) {
        String jsonSearch = String.format(searchStringTemplate, count, hits, aggregations);
        SearchResult mockSearchResult = new SearchResult(new Gson());
        mockSearchResult.setJsonString(jsonSearch);
        mockSearchResult.setSucceeded(true);
        mockSearchResult.setJsonObject(new JsonParser().parse(jsonSearch).getAsJsonObject());
        mockSearchResult.setPathToResult("hits/hits/_source");

        when(mock.searchES(eq(typeES), anyString())).thenReturn(mockSearchResult);
    }

    public static void configSuggest(RuntimeJestClientAdapter mock, String content) {
        String jsonSearch = content;
        JestResult mockSuggestResult = new JestResult(new Gson());
        mockSuggestResult.setJsonString(jsonSearch);
        mockSuggestResult.setSucceeded(true);
        mockSuggestResult.setJsonObject(new JsonParser().parse(jsonSearch).getAsJsonObject());
        mockSuggestResult.setPathToResult("");

        when(mock.suggestES(anyString())).thenReturn(mockSuggestResult);
    }

    public static void configGet(RuntimeJestClientAdapter mock, String typeES, String content) {
        String jsonGet = content;
        JestResult mockGetResult = new JestResult(new Gson());

        mockGetResult.setJsonString(jsonGet);
        mockGetResult.setSucceeded(true);
        mockGetResult.setJsonObject(new JsonParser().parse(jsonGet).getAsJsonObject());
        mockGetResult.setPathToResult("_source");

        when(mock.getES(eq(typeES), anyString())).thenReturn(mockGetResult);
    }
}
