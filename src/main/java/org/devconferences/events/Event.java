package org.devconferences.events;

public class Event {

    public String id;
    public String name;
    public String avatar;
    public String description;
    public String website;
    public String twitter;
    public String facebook;
    public String meetup;
    public String city;
    public Type type;


    public enum Type{
        COMMUNITY, CONFERENCE;
    }
}
