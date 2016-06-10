package org.devconferences.events;

import java.util.ArrayList;
import java.util.List;

import org.devconferences.elastic.Completion;
import org.elasticsearch.common.geo.GeoPoint;

public class Event {

    public String id;
    public String name;
    public String avatar;
    public String description;
    public String website;
    public String twitter;
    public String facebook;
    public String meetup;
    public Youtube youtube;
    public String parleys;
    public String city;
    public List<String> tags = new ArrayList<>();
    public Type type;
    public GeoPoint gps;

    public Event() {

    }

    public Event(Event obj) {
        id = obj.id;
        name = obj.name;
        avatar = obj.avatar;
        description = obj.description;
        website = obj.website;
        twitter = obj.twitter;
        facebook = obj.facebook;
        meetup = obj.meetup;
        if(obj.youtube != null) {
            youtube = new Youtube();
            youtube.channel = obj.youtube.channel;
            youtube.name = obj.youtube.name;
        }
        parleys = obj.parleys;
        city = obj.city;
        tags = new ArrayList<>(obj.tags);
        type = obj.type;
        if(obj.gps != null) {
            gps = new GeoPoint(obj.gps.lat(), obj.gps.lon());
        }
    }

    public class Youtube {
        public String channel;
        public String name;
    }

    public enum Type{
        COMMUNITY, CONFERENCE
    }
}
