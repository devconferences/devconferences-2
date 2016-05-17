package org.devconferences.meetup;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Singleton;
import net.codestory.http.constants.Headers;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicHeader;
import org.devconferences.events.CalendarEvent;
import org.jose4j.json.internal.json_simple.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class MeetupApiClient {

    public static final String MEETUP_API_KEY = "MEETUP_API_KEY";
    public static final String MEETUP_API_BASE_URL = "https://api.meetup.com";
    public static final String MEETUP_API_GROUP_INFO_URL = MEETUP_API_BASE_URL + "/%s?sign=true&key=%s";
    public static final String MEETUP_API_EVENT_INFO_URL = MEETUP_API_BASE_URL + "/2/event/%s?sign=true&key=%s";
    public static final String MEETUP_API_EVENTS_BY_GROUP_URL = MEETUP_API_BASE_URL + "/2/events/?group_urlname=%s&" +
            "status=upcoming&sign=true&key=%s";

    private Cache<String, MeetupInfo> cache;

    public MeetupApiClient() {
        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }

    public MeetupInfo getMeetupInfo(String id) {
        try {
            return cache.get(id, () -> loadMeetupInfo(id));
        } catch (ExecutionException e) {
            return null;
        }
    }

    public List<CalendarEvent> getUpcomingEvents(String id) throws Exception {
        Content eventsByURLResponse = Request.Get(String.format(MEETUP_API_EVENTS_BY_GROUP_URL, id,
                System.getenv(MEETUP_API_KEY)))
                .addHeader(new BasicHeader(Headers.ACCEPT, "application/json"))
                .execute()
                .returnContent();

        EventsSearch eventsSearch = new Gson().fromJson(eventsByURLResponse.asString(), EventsSearch.class);

        List<CalendarEvent> result = new ArrayList<>();

        eventsSearch.results.forEach(data -> {
            CalendarEvent calendarEvent = new CalendarEvent();
            calendarEvent.id = "meetup_" + data.id;
            calendarEvent.name = data.name;
            calendarEvent.url = data.event_url;
            calendarEvent.description = data.description;
            calendarEvent.date = data.time;
            calendarEvent.duration = data.duration;
            calendarEvent.organizerName = data.group.name;
            calendarEvent.organizerUrl = "http://www.meetup.com/" + data.group.urlname;

            result.add(calendarEvent);
        });

        return result;
    }


    //TODO improve error management
    private MeetupInfo loadMeetupInfo(String id) throws Exception {
        MeetupInfo meetupInfo = new MeetupInfo();

        // Step 1 - get group data
        Content groupInfoResponse = Request.Get(String.format(MEETUP_API_GROUP_INFO_URL, id, System.getenv(MEETUP_API_KEY)))
                .addHeader(new BasicHeader(Headers.ACCEPT, "application/json"))
                .execute()
                .returnContent();
        Group group = new Gson().fromJson(groupInfoResponse.asString(), Group.class);
        meetupInfo.name = group.name;
        meetupInfo.url = group.link;
        meetupInfo.members = group.members;

        // Step 2 - get next event data
        if (group.next_event != null) {
            String nextEventId = group.next_event.id;
            Content eventInfoResponse = Request.Get(String.format(MEETUP_API_EVENT_INFO_URL, nextEventId, System.getenv(MEETUP_API_KEY)))
                    .addHeader(new BasicHeader(Headers.ACCEPT, "application/json"))
                    .execute()
                    .returnContent();
            Event nextEvent = new Gson().fromJson(eventInfoResponse.asString(), Event.class);

            meetupInfo.nextEvent = new MeetupInfo.NextEventInfo();
            meetupInfo.nextEvent.name = nextEvent.name;
            meetupInfo.nextEvent.url = nextEvent.event_url;
            meetupInfo.nextEvent.time = nextEvent.time;

        }

        return meetupInfo;
    }
}
