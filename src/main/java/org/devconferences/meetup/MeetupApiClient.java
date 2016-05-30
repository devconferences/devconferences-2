package org.devconferences.meetup;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.inject.Singleton;
import net.codestory.http.constants.Headers;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicHeader;
import org.devconferences.events.CalendarEvent;
import org.elasticsearch.common.geo.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

@Singleton
public class MeetupApiClient {
    private Cache<String, MeetupInfo> cache;
    private final MeetupCalls meetupCall;

    public MeetupApiClient() {
        this(new MeetupCalls());
    }

    public MeetupApiClient(MeetupCalls meetupCall) {
        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();

        this.meetupCall = meetupCall;
    }

    public MeetupInfo getMeetupInfo(String id) {
        try {
            return cache.get(id, () -> loadMeetupInfo(id));
        } catch (ExecutionException e) {
            return null;
        }
    }

    public List<CalendarEvent> getUpcomingEvents(String id) {
        EventsSearch eventsSearch = meetupCall.askUpcomingEvents(id);

        List<CalendarEvent> result = new ArrayList<>();

        eventsSearch.results.forEach(data -> {
            CalendarEvent calendarEvent = new CalendarEvent();
            calendarEvent.id = "meetup_" + data.id;
            calendarEvent.name = data.name;
            calendarEvent.url = data.event_url;
            calendarEvent.description = data.description;
            calendarEvent.date = data.time;
            calendarEvent.duration = data.duration;
            if(data.group != null) {
                calendarEvent.organizer = calendarEvent.new Group();
                calendarEvent.organizer.name = data.group.name;
                calendarEvent.organizer.url = "http://www.meetup.com/" + data.group.urlname;
            }

            if(data.venue != null) {
                calendarEvent.location = calendarEvent.new Location();
                calendarEvent.location.address = data.venue.address_1;
                calendarEvent.location.name = data.venue.name;
                calendarEvent.location.city = data.venue.city;
                calendarEvent.location.gps = new GeoPoint(data.venue.lat, data.venue.lon);
            }
            result.add(calendarEvent);
        });

        return result;
    }


    //TODO improve error management
    private MeetupInfo loadMeetupInfo(String id) throws Exception {
        MeetupInfo meetupInfo = new MeetupInfo();

        // Step 1 - get group data
        Group group = meetupCall.askGroupInfo(id);
        meetupInfo.name = group.name;
        meetupInfo.url = group.link;
        meetupInfo.members = group.members;

        // Step 2 - get next event data
        if (group.next_event != null) {
            String nextEventId = group.next_event.id;
            Event nextEvent = meetupCall.askEventInfo(nextEventId);

            meetupInfo.nextEvent = new MeetupInfo.NextEventInfo();
            meetupInfo.nextEvent.name = nextEvent.name;
            meetupInfo.nextEvent.url = nextEvent.event_url;
            meetupInfo.nextEvent.time = nextEvent.time;

        }

        return meetupInfo;
    }
}
