package org.devconferences.events;

import org.elasticsearch.common.geo.GeoPoint;

public class CalendarEvent {
    public String id;
    public String name;
    public long date;
    public long duration;
    public String url;
    public String description;
    public Group organizer;
    public Location location;
    public CallForPapers cfp;

    public CalendarEvent() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CalendarEvent that = (CalendarEvent) o;

        if (date != that.date) return false;
        if (duration != that.duration) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (organizer != null ? !organizer.equals(that.organizer) : that.organizer != null) return false;
        if (location != null ? !location.equals(that.location) : that.location != null) return false;
        return cfp != null ? cfp.equals(that.cfp) : that.cfp == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (int) (date ^ (date >>> 32));
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (organizer != null ? organizer.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (cfp != null ? cfp.hashCode() : 0);
        return result;
    }

    public CalendarEvent(CalendarEvent obj) {
        id = obj.id;
        name = obj.name;
        date = obj.date;
        duration = obj.duration;
        url = obj.url;
        description = obj.description;
        if(obj.organizer != null) {
            organizer = new Group();
            organizer.name = obj.organizer.name;
            organizer.url = obj.organizer.url;
        }
        if(obj.location != null) {
            location = new Location();
            location.city = obj.location.city;
            location.address = obj.location.address;
            location.gps = new GeoPoint(obj.location.gps.lat(), obj.location.gps.lon());
            location.name = obj.location.name;
        }
        if(obj.cfp != null) {
            cfp = new CallForPapers();
            cfp.dateSubmission = obj.cfp.dateSubmission;
            cfp.url = obj.cfp.url;
        }
    }

    public class Group {
        public String name;
        public String url;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Group group = (Group) o;

            if (name != null ? !name.equals(group.name) : group.name != null) return false;
            return url != null ? url.equals(group.url) : group.url == null;

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (url != null ? url.hashCode() : 0);
            return result;
        }
    }

    public class Location {
        public String city;
        public String name;
        public String address;
        public GeoPoint gps;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Location location = (Location) o;

            if (city != null ? !city.equals(location.city) : location.city != null) return false;
            if (name != null ? !name.equals(location.name) : location.name != null) return false;
            if (address != null ? !address.equals(location.address) : location.address != null) return false;
            return gps != null ? gps.equals(location.gps) : location.gps == null;

        }

        @Override
        public int hashCode() {
            int result = city != null ? city.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (address != null ? address.hashCode() : 0);
            result = 31 * result + (gps != null ? gps.hashCode() : 0);
            return result;
        }
    }

    public class CallForPapers {
        public String url;
        public Long dateSubmission;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CallForPapers that = (CallForPapers) o;

            if (url != null ? !url.equals(that.url) : that.url != null) return false;
            return dateSubmission != null ? dateSubmission.equals(that.dateSubmission) : that.dateSubmission == null;
        }

        @Override
        public int hashCode() {
            int result = url != null ? url.hashCode() : 0;
            result = 31 * result + (dateSubmission != null ? dateSubmission.hashCode() : 0);
            return result;
        }
    }
}
