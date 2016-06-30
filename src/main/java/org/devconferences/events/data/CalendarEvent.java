package org.devconferences.events.data;

import org.elasticsearch.common.geo.GeoPoint;

public class CalendarEvent extends AbstractEvent {
    public long date;
    public long duration;
    public Group organizer;
    public Location location;
    public CallForPapers cfp;

    public CalendarEvent() {
        super();
    }

    public CalendarEvent(CalendarEvent obj) {
        super(obj);

        date = obj.date;
        duration = obj.duration;
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

    public CalendarEvent(org.devconferences.meetup.Event meetupEvent) {
        this.id = "meetup_" + meetupEvent.id;
        this.name = meetupEvent.name;
        this.url = meetupEvent.event_url;
        this.description = meetupEvent.description;
        this.date = meetupEvent.time;
        this.duration = meetupEvent.duration;
        if(meetupEvent.group != null) {
            this.organizer = new Group();
            this.organizer.name = meetupEvent.group.name;
            this.organizer.url = "http://www.meetup.com/" + meetupEvent.group.urlname;
        }

        if(meetupEvent.venue != null) {
            this.location = new Location();
            this.location.address = meetupEvent.venue.address_1;
            this.location.name = meetupEvent.venue.name;
            this.location.city = meetupEvent.venue.city;
            this.location.gps = new GeoPoint(meetupEvent.venue.lat, meetupEvent.venue.lon);
        }
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        if(!super.equals(o)) return false;

        CalendarEvent that = (CalendarEvent) o;

        if(date != that.date) return false;
        if(duration != that.duration) return false;
        if(organizer != null ? !organizer.equals(that.organizer) : that.organizer != null) return false;
        if(location != null ? !location.equals(that.location) : that.location != null) return false;
        return cfp != null ? cfp.equals(that.cfp) : that.cfp == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (date ^ (date >>> 32));
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        result = 31 * result + (organizer != null ? organizer.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (cfp != null ? cfp.hashCode() : 0);
        return result;
    }

    public class Group {
        public String name;
        public String url;

        @Override
        public boolean equals(Object o) {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;

            Group group = (Group) o;

            if(name != null ? !name.equals(group.name) : group.name != null) return false;
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
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;

            Location location = (Location) o;

            if(city != null ? !city.equals(location.city) : location.city != null) return false;
            if(name != null ? !name.equals(location.name) : location.name != null) return false;
            if(address != null ? !address.equals(location.address) : location.address != null) return false;
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
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;

            CallForPapers that = (CallForPapers) o;

            if(url != null ? !url.equals(that.url) : that.url != null) return false;
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
