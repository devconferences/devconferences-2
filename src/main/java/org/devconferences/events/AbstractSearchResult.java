package org.devconferences.events;

import java.util.List;

public abstract class AbstractSearchResult<T> {
    public String query;
    public String lat;
    public String lon;
    public String distance;
    public String totalHits;
    public String hitsAPage;
    public String totalPage;
    public String currPage;
    public List<T> hits;

}
