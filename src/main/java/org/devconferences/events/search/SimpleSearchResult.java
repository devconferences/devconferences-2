package org.devconferences.events.search;

import io.searchbox.core.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleSearchResult<T> {
    public String query;
    public final List<T> hits = new ArrayList<T>();

    public List<T> getHitsFromSearch(SearchResult searchResult, Class<T> clazz) {
        return (searchResult.getHits(clazz).stream()
                .map((data) -> data.source).collect(Collectors.toList())
        );
    }
}
