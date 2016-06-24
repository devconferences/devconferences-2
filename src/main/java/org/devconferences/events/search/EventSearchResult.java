package org.devconferences.events.search;

import io.searchbox.core.SearchResult;
import org.devconferences.events.Event;

import java.util.List;

public class EventSearchResult extends PaginatedSearchResult<Event> {
    public List<Event> getHitsFromSearch(SearchResult searchResult) {
        return super.getHitsFromSearch(searchResult, Event.class);
    }
}
