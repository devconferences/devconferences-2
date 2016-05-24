package org.devconferences.events;

import org.elasticsearch.common.geo.GeoPoint;

import java.util.List;

public class City {

    public String id;
    public String name;
    public List<Event> conferences;
    public List<Event> communities;

}
