package org.devconferences.events;

import org.elasticsearch.common.geo.GeoPoint;

public class CalendarEvent {
    public String id;
    public String name;
    public long date;
    public long duration;
    public String url;
    public String description;
    public String organizerName;
    public String organizerUrl;
    public Location location;
    public CallForPapers cfp;

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
