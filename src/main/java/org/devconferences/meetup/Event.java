package org.devconferences.meetup;

public class Event {
    public String id;
    public String name;
    public String description;
    public String event_url;
    public Location venue;
    public long time;
    public long duration;
    public Organizer group;

    public static class Organizer {
        public String name;
        public String urlname;
    }

    public static class Location {
        public String city;
        public String name;
        public String address_1;
        public double lat;
        public double lon;
    }
}
