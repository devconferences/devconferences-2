package org.devconferences.meetup;

public class MeetupInfo {

    public String name;
    public String url;
    public int members;
    public NextEventInfo nextEvent;

    public static class NextEventInfo {
        public String name;
        public String description;
        public String url;
        public long time;
    }

}
