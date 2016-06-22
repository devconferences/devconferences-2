package org.devconferences.events;

import org.devconferences.elastic.Completion;

public class ESEvents extends Event {
    public final Completion<String> suggests = new Completion<>();

    public ESEvents() {
        super();
    }

    public ESEvents(Event obj) {
        super(obj);
    }
}
