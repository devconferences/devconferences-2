package org.devconferences.events;

import org.elasticsearch.common.geo.GeoPoint;

public class CityLight implements Comparable {

    public final String id;
    public final String name;
    public long count;
    public long totalCommunity;
    public long totalConference;
    public long totalCalendar;
    public GeoPoint location;

    public CityLight(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int compareTo(Object o) {
        CityLight other = (CityLight) o;
        return this.name.compareTo(other.name);
    }
}
