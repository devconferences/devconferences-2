package org.devconferences.events.data;

import org.elasticsearch.common.geo.GeoPoint;

import java.util.List;

public class City {

    public String id;
    public String name;
    public GeoPoint location;
    public List<Event> conferences;
    public List<Event> communities;
    public List<CalendarEvent> upcoming_events;

}
