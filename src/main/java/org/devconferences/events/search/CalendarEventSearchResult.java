package org.devconferences.events.search;

import io.searchbox.core.SearchResult;
import org.devconferences.events.CalendarEvent;

import java.util.List;

public class CalendarEventSearchResult extends PaginatedSearchResult<CalendarEvent> {
    public List<CalendarEvent> getHitsFromSearch(SearchResult searchResult) {
        return getHitsFromSearch(searchResult, CalendarEvent.class);
    }
}
