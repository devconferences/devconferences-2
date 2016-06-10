package org.devconferences.events.search;

public abstract class PaginatedSearchResult<T> extends SimpleSearchResult<T> {
    public String totalHits;
    public String hitsAPage;
    public String totalPage;
    public String currPage;
}
