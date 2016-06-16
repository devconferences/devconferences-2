package org.devconferences.events.search;

import java.util.List;

public class SimpleSearchResult<T> {
    public String query;
    public List<T> hits;
}
