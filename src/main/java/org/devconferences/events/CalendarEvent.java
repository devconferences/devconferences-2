package org.devconferences.events;

import org.devconferences.elastic.Completion;
import org.elasticsearch.common.geo.GeoPoint;

import java.util.List;

public class CalendarEvent {
    public String id;
    public String name;
    public Completion<List<String>> name_calendar_suggest = new Completion<>();
    public long date;
    public long duration;
    public String url;
    public String description;
    public Group organizer;
    public Location location;
    public CallForPapers cfp;

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
