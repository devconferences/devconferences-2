package org.devconferences.events;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.common.geo.GeoPoint;

public class Event {

    public String id;
    public String name;
    public String avatar;
    public String description;
    public String url;
    public String twitter;
    public String facebook;
    public String meetup;
    public Youtube youtube;
    public String parleys;
    public String city;
    public List<String> tags = new ArrayList<>();
    public Type type;
    public GeoPoint gps;
    public Boolean hidden;

    public Event() {

    }

    public Event(Event obj) {
        id = obj.id;
        name = obj.name;
        avatar = obj.avatar;
        description = obj.description;
        url = obj.url;
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
        hidden = obj.hidden;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Event event = (Event) o;

        if (id != null ? !id.equals(event.id) : event.id != null) return false;
        if (name != null ? !name.equals(event.name) : event.name != null) return false;
        if (avatar != null ? !avatar.equals(event.avatar) : event.avatar != null) return false;
        if (description != null ? !description.equals(event.description) : event.description != null) return false;
        if (url != null ? !url.equals(event.url) : event.url != null) return false;
        if (twitter != null ? !twitter.equals(event.twitter) : event.twitter != null) return false;
        if (facebook != null ? !facebook.equals(event.facebook) : event.facebook != null) return false;
        if (meetup != null ? !meetup.equals(event.meetup) : event.meetup != null) return false;
        if (youtube != null ? !youtube.equals(event.youtube) : event.youtube != null) return false;
        if (parleys != null ? !parleys.equals(event.parleys) : event.parleys != null) return false;
        if (city != null ? !city.equals(event.city) : event.city != null) return false;
        if (tags != null ? !tags.equals(event.tags) : event.tags != null) return false;
        if (hidden != null ? !hidden.equals(event.hidden) : event.hidden != null) return false;
        if (type != event.type) return false;
        return gps != null ? gps.equals(event.gps) : event.gps == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (avatar != null ? avatar.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (twitter != null ? twitter.hashCode() : 0);
        result = 31 * result + (facebook != null ? facebook.hashCode() : 0);
        result = 31 * result + (meetup != null ? meetup.hashCode() : 0);
        result = 31 * result + (youtube != null ? youtube.hashCode() : 0);
        result = 31 * result + (parleys != null ? parleys.hashCode() : 0);
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (gps != null ? gps.hashCode() : 0);
        result = 31 * result + (hidden != null ? hidden.hashCode() : 0);
        return result;
    }

    public class Youtube {
        public String channel;
        public String name;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Youtube youtube = (Youtube) o;

            if (channel != null ? !channel.equals(youtube.channel) : youtube.channel != null) return false;
            return name != null ? name.equals(youtube.name) : youtube.name == null;

        }

        @Override
        public int hashCode() {
            int result = channel != null ? channel.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }

    public enum Type{
        COMMUNITY, CONFERENCE
    }
}
