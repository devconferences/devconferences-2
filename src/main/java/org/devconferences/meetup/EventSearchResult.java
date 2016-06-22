package org.devconferences.meetup;

import java.util.List;

public class EventSearchResult {
    public List<EventSearchResultItem> results;

    public class EventSearchResultItem {
        public String id;
        public String name;
        public String description;
        public String event_url;
        public Location venue;
        public long time;
        public long duration;
        public Organizer group;
    }

    public class Organizer {
        public String name;
        public String urlname;
    }

    public class Location {
        public String city;
        public String name;
        public String address_1;
        public double lat;
        public double lon;
    }

}