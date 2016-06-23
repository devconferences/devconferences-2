package org.devconferences.events.search;

import java.util.ArrayList;
import java.util.List;

public class SimpleSearchResult<T> {
    public String query;
    public final List<T> hits = new ArrayList<T>();
}
