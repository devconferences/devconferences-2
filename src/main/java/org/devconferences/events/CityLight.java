package org.devconferences.events;

import org.elasticsearch.common.geo.GeoPoint;

class CityAggreg {
    public String name;
    public long count;
}

public class CityLight {

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

    public CityLight(String id, String name, long count) {
        this(id, name);
        this.count = count;
    }

    public CityLight(String id, String name, long count, GeoPoint location) {
        this(id, name);
        this.count = count;
        this.location = location;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }
}
