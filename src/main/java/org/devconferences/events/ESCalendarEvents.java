package org.devconferences.events;

import org.devconferences.elastic.Completion;

public class ESCalendarEvents extends CalendarEvent {
    public Completion<String> suggests = new Completion<>();

    public ESCalendarEvents() {
        super();
    }

    public ESCalendarEvents(CalendarEvent obj) {
        super(obj);
    }
}
