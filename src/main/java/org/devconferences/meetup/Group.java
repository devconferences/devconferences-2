package org.devconferences.meetup;

public class Group {

    public String name;
    public String link;
    public int members;
    public MeetupApiNextEvent next_event;

    public class MeetupApiNextEvent {
        public String id;
    }
}
