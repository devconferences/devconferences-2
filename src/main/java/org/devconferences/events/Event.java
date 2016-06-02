package org.devconferences.events;

import java.util.ArrayList;
import java.util.List;

import org.devconferences.elastic.Completion;
import org.elasticsearch.common.geo.GeoPoint;

public class Event {

    public String id;
    public String name;
    public Completion<List<String>> name_suggest = new Completion<>();
    public String avatar;
    public String description;
    public String website;
    public String twitter;
    public String facebook;
    public String meetup;
    public Youtube youtube;
    public String parleys;
    public String city;
    public Completion<String> city_suggest = new Completion<>();
    public List<String> tags = new ArrayList<>();
    public Completion<List<String>> tags_suggest = new Completion<>();
    public Type type;
    public GeoPoint location;

    public class Youtube {
        public String channel;
        public String name;
    }


    public enum Type{
        COMMUNITY, CONFERENCE;
    }
}
