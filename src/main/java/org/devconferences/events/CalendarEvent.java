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
    }

    public class Location {
        public String city;
        public String name;
        public String address;
        public GeoPoint gps;
    }

    public class CallForPapers {
        public String url;
        public Long dateSubmission;
    }
}
