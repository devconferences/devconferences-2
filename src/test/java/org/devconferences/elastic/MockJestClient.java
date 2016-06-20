package org.devconferences.elastic;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import io.searchbox.client.JestResult;
import io.searchbox.core.*;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.devconferences.events.EventsRepository.CALENDAREVENTS_TYPE;
import static org.devconferences.events.EventsRepository.EVENTS_TYPE;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Create and configure mocks of RuntimeJestClientAdapter for tests.
 */
public class MockJestClient {

    private static final class SearchCalendarMatcher extends ArgumentMatcher<Search> {
        @Override
        public boolean matches(Object o) {
            if(o instanceof Search) {
                Search search = (Search) o;
                return search.getType().equals(CALENDAREVENTS_TYPE);
            }
            return false;
        }

        public String toString() {
            return "{Search object on " + CALENDAREVENTS_TYPE + " type}";
        }
    }

    private static final class SearchEventsMatcher extends ArgumentMatcher<Search> {
        @Override
        public boolean matches(Object o) {
            if(o instanceof Search) {
                Search search = (Search) o;
                return search.getType().equals(EVENTS_TYPE);
            }
            return false;
        }

        public String toString() {
            return "{Search object on " + EVENTS_TYPE + " type}";
        }
    }

    // Explanation of this in test of Jest-client, e.g :
    // https://github.com/searchbox-io/Jest/blob/master/jest-common/src/test/java/io/searchbox/core/SearchResultTest.java
    private static final String countStringTemplate = "" +
            "{" +
            "    \"count\": %s," +
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
    public static RuntimeJestClientAdapter createMock() {
        RuntimeJestClientAdapter result = mock(RuntimeJestClientAdapter.class);
        return result;
    }

    public static void configCount(RuntimeJestClientAdapter mock, int count) {
        String jsonCount = String.format(countStringTemplate, count);
        CountResult mockCountResult = new CountResult(new Gson());
        configJestResult(mockCountResult, jsonCount, true, "count");

        when(mock.execute(isA(Count.class))).thenReturn(mockCountResult);
    }

    public static void configSearch(RuntimeJestClientAdapter mock, int count, String hits, String aggregations) {
        String jsonSearch = String.format(searchStringTemplate, count, hits, aggregations);
        SearchResult mockSearchResult = new SearchResult(new Gson());
        configJestResult(mockSearchResult, jsonSearch, true, "hits/hits/_source");

        when(mock.execute(isA(Search.class))).thenReturn(mockSearchResult);
    }

    public static void configSearch(RuntimeJestClientAdapter mock, String type, int count, String hits, String aggregations) {
        String jsonSearch = String.format(searchStringTemplate, count, hits, aggregations);
        SearchResult mockSearchResult = new SearchResult(new Gson());
        configJestResult(mockSearchResult, jsonSearch, true, "hits/hits/_source");

        switch(type) {
            case EVENTS_TYPE:
                when(mock.execute(argThat(new SearchEventsMatcher()))).thenReturn(mockSearchResult);
                break;
            case CALENDAREVENTS_TYPE:
                when(mock.execute(argThat(new SearchCalendarMatcher()))).thenReturn(mockSearchResult);
        }
    }

    public static void configSuggest(RuntimeJestClientAdapter mock, String content) {
        String jsonSearch = content;
        SuggestResult mockSuggestResult = new SuggestResult(new Gson());
        configJestResult(mockSuggestResult, jsonSearch, true, "");

        when(mock.execute(isA(Suggest.class))).thenReturn(mockSuggestResult);
    }

    public static void configGet(RuntimeJestClientAdapter mock, String content) {
        String jsonGet = content;
        DocumentResult mockGetResult = new DocumentResult(new Gson());
        configJestResult(mockGetResult, jsonGet, true, "_source");

        when(mock.execute(isA(Get.class))).thenReturn(mockGetResult);
    }

    public static void configUpdate(RuntimeJestClientAdapter mock, String content) {
        String jsonSearch = content;
        DocumentResult mockUpdateResult = new DocumentResult(new Gson());
        configJestResult(mockUpdateResult, jsonSearch, true, "");

        when(mock.execute(isA(Update.class))).thenReturn(mockUpdateResult);
    }

    public static void configJestResult(JestResult mockJestResult, String jsonString, boolean isSucceeded, String pathToResult) {
        mockJestResult.setJsonString(jsonString);
        mockJestResult.setSucceeded(isSucceeded);
        mockJestResult.setJsonObject(new JsonParser().parse(jsonString).getAsJsonObject());
        mockJestResult.setPathToResult(pathToResult);
    }
}
