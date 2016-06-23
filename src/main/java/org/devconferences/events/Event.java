package org.devconferences.events;

import org.elasticsearch.common.geo.GeoPoint;

public class Event extends AbstractEvent {
    public String avatar;
    public String twitter;
    public String facebook;
    public String meetup;
    public String parleys;
    public String city;
    public Type type;
    public Youtube youtube;
    public GeoPoint gps;

    public Event() {
        super();
    }

    public Event(Event obj) {
        super(obj);
        avatar = obj.avatar;
        twitter = obj.twitter;
        facebook = obj.facebook;
        meetup = obj.meetup;
        parleys = obj.parleys;
        city = obj.city;
        type = obj.type;
        if(obj.youtube != null) {
            youtube = new Youtube();
            youtube.channel = obj.youtube.channel;
            youtube.name = obj.youtube.name;
        }
        if(obj.gps != null) {
            gps = new GeoPoint(obj.gps.lat(), obj.gps.lon());
        }
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        if(!super.equals(o)) return false;

        Event event = (Event) o;

        if(avatar != null ? !avatar.equals(event.avatar) : event.avatar != null) return false;
        if(twitter != null ? !twitter.equals(event.twitter) : event.twitter != null) return false;
        if(facebook != null ? !facebook.equals(event.facebook) : event.facebook != null) return false;
        if(meetup != null ? !meetup.equals(event.meetup) : event.meetup != null) return false;
        if(youtube != null ? !youtube.equals(event.youtube) : event.youtube != null) return false;
        if(parleys != null ? !parleys.equals(event.parleys) : event.parleys != null) return false;
        if(city != null ? !city.equals(event.city) : event.city != null) return false;
        if(type != event.type) return false;
        return gps != null ? gps.equals(event.gps) : event.gps == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (avatar != null ? avatar.hashCode() : 0);
        result = 31 * result + (twitter != null ? twitter.hashCode() : 0);
        result = 31 * result + (facebook != null ? facebook.hashCode() : 0);
        result = 31 * result + (meetup != null ? meetup.hashCode() : 0);
        result = 31 * result + (youtube != null ? youtube.hashCode() : 0);
        result = 31 * result + (parleys != null ? parleys.hashCode() : 0);
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (gps != null ? gps.hashCode() : 0);
        return result;
    }

    public class Youtube {
        public String channel;
        public String name;

        @Override
        public boolean equals(Object o) {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;

            Youtube youtube = (Youtube) o;

            if(channel != null ? !channel.equals(youtube.channel) : youtube.channel != null) return false;
            return name != null ? name.equals(youtube.name) : youtube.name == null;

        }

        @Override
        public int hashCode() {
            int result = channel != null ? channel.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }

    public enum Type {
        COMMUNITY, CONFERENCE
    }
}
