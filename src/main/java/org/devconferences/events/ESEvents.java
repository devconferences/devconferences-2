package org.devconferences.events;

import org.devconferences.elastic.Completion;

import java.util.List;

public class ESEvents extends Event {
    public Completion<List<String>> name_event_suggest = new Completion<>();
    public Completion<String> city_event_suggest = new Completion<>();
    public Completion<List<String>> tags_event_suggest = new Completion<>();

    public ESEvents() {

    }

    public ESEvents(Event obj) {
        super(obj);
    }
}
