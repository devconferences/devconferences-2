package org.devconferences.meetup;

import com.google.gson.Gson;
import net.codestory.http.constants.Headers;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicHeader;

import java.io.IOException;

/**
 * Created by ronan on 21/06/16.
 */
class MeetupCalls {
    public static final String MEETUP_API_KEY = "MEETUP_API_KEY";
    public static final String MEETUP_API_BASE_URL = "https://api.meetup.com";
    public static final String MEETUP_API_GROUP_INFO_URL = MEETUP_API_BASE_URL + "/%s?sign=true&key=%s";
    public static final String MEETUP_API_EVENT_INFO_URL = MEETUP_API_BASE_URL + "/2/event/%s?sign=true&key=%s";
    public static final String MEETUP_API_EVENTS_BY_GROUP_URL = MEETUP_API_BASE_URL + "/2/events/?group_urlname=%s&" +
            "status=upcoming&sign=true&key=%s";

    public EventsSearch askUpcomingEvents(String id) {
        return (EventsSearch) getContent(MEETUP_API_EVENTS_BY_GROUP_URL, id, EventsSearch.class);
    }

    public Group askGroupInfo(String id) {
        return (Group) getContent(MEETUP_API_GROUP_INFO_URL, id, Group.class);
    }

    public Event askEventInfo(String id) {
        return (Event) getContent(MEETUP_API_EVENT_INFO_URL, id, Event.class);
    }

    private Object getContent(String urlFormat, String id, Class classType) {
        Content eventsByURLResponse;
        try {
            eventsByURLResponse = Request.Get(String.format(urlFormat, id, System.getenv(MEETUP_API_KEY)))
                    .addHeader(new BasicHeader(Headers.ACCEPT, "application/json")).execute().returnContent();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new Gson().fromJson(eventsByURLResponse.asString(), classType);
    }
}
