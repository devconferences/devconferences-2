package org.devconferences.events;

import org.elasticsearch.common.geo.GeoPoint;

public class Event {

    public String id;
    public String name;
    public String avatar;
    public String description;
    public String website;
    public String twitter;
    public String facebook;
    public String city;
    public Type type;
    public GeoPoint location;

    public enum Type{
        COMMUNITY, CONFERENCE;
    }
}
