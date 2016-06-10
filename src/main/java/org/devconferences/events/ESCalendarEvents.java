package org.devconferences.events;

import org.devconferences.elastic.Completion;

import java.util.List;

public class ESCalendarEvents extends CalendarEvent {
    public Completion<List<String>> name_calendar_suggest = new Completion<>();

    public ESCalendarEvents() {

    }

    public ESCalendarEvents(CalendarEvent obj) {
        super(obj);
    }
}
