package org.devconferences.meetup;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Singleton;
import net.codestory.http.errors.HttpException;
import org.devconferences.events.data.CalendarEvent;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class MeetupApiClient {
    private Cache<String, MeetupInfo> cache;
    private final MeetupCalls meetupCall;

    public MeetupApiClient() {
        this(new MeetupCalls());
    }

    MeetupApiClient(MeetupCalls meetupCall) {
        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();

        this.meetupCall = meetupCall;
    }

    MeetupInfo getMeetupInfo(String id) {
        try {
            return cache.get(id, () -> loadMeetupInfo(id));
        } catch(ExecutionException e) {
            return null;
        } catch(UncheckedExecutionException e) {
            // HTML Errors when use Meetup API
            if(e.getCause() instanceof HttpException) {
                throw (HttpException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    public List<CalendarEvent> getUpcomingEvents(String id) {
        EventSearchResult eventSearchResult = meetupCall.getUpcomingEvents(id);

        return eventSearchResult.results.stream()
                .map(CalendarEvent::new).collect(Collectors.toList());
    }

    private MeetupInfo loadMeetupInfo(String id) throws Exception {
        MeetupInfo meetupInfo = new MeetupInfo();

        // Step 1 - get group data
        Group group = meetupCall.getGroupInfo(id);
        meetupInfo.name = group.name;
        meetupInfo.url = group.link;
        meetupInfo.members = group.members;

        // Step 2 - get next event data
        if(group.next_event != null) {
            String nextEventId = group.next_event.id;
            Event nextEvent = meetupCall.getEventInfo(nextEventId);

            meetupInfo.nextEvent = new MeetupInfo.NextEventInfo();
            meetupInfo.nextEvent.name = nextEvent.name;
            meetupInfo.nextEvent.url = nextEvent.event_url;
            meetupInfo.nextEvent.time = nextEvent.time;

        }

        return meetupInfo;
    }
}
