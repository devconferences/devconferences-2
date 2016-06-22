package org.devconferences.meetup;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Singleton;
import org.devconferences.events.ESCalendarEvents;
import org.elasticsearch.common.geo.GeoPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

    public List<ESCalendarEvents> getUpcomingEvents(String id) {
        EventSearchResult eventSearchResult = meetupCall.askUpcomingEvents(id);

        List<ESCalendarEvents> result = new ArrayList<>();

        eventSearchResult.results.forEach(data -> {
            ESCalendarEvents calendarEvent = new ESCalendarEvents();
            calendarEvent.id = "meetup_" + data.id;
            calendarEvent.name = data.name;
            calendarEvent.suggests.input = new ArrayList<>();
            calendarEvent.suggests.input.addAll(Arrays.asList(calendarEvent.name.split(" ")));
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
