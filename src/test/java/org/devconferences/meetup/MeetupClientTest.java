package org.devconferences.meetup;

import org.assertj.core.api.Assertions;
import org.devconferences.events.ESCalendarEvents;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MeetupClientTest {
    private MeetupApiClient meetupApiClient;
    private MeetupEndPoint meetupEndPoint;
    private MeetupCalls mockMeetupCalls;

    @Before
    public void setUp() {
        mockMeetupCalls = mock(MeetupCalls.class);
        meetupApiClient = new MeetupApiClient(mockMeetupCalls);
        meetupEndPoint = new MeetupEndPoint(meetupApiClient);
    }

    @Test
    public void testUpcomingEvents() {
        EventSearchResult eventSearchResultId1 = new EventSearchResult();

        EventSearchResult.EventSearchResultItem eventSearch1 = eventSearchResultId1.new EventSearchResultItem();
        eventSearch1.id = "azerty";
        eventSearch1.name = "Azerty";
        eventSearch1.description = "AZERTY.";
        eventSearch1.time = 123456789000L;
        eventSearch1.group = eventSearchResultId1.new Organizer();
        eventSearch1.group.name = "AzErTy";
        eventSearch1.group.urlname = "azerty1";
        eventSearch1.venue = eventSearchResultId1.new Location();
        eventSearch1.venue.address_1 = "1, Rue Bidon";
        eventSearch1.venue.city = "Ville Bidon";
        eventSearch1.venue.lat = 12.3456;
        eventSearch1.venue.lon = 34.5678;
        EventSearchResult.EventSearchResultItem eventSearch2 = eventSearchResultId1.new EventSearchResultItem();
        eventSearch2.id = "qsdfgh";
        eventSearch2.name = "Qsdfgh";
        eventSearch2.description = "QSDFGH.";
        eventSearch2.time = 123456987000L;
        eventSearch2.group = eventSearchResultId1.new Organizer();
        eventSearch2.group.name = "QsDfGh";
        eventSearch2.group.urlname = "qsdfgh1";

        eventSearchResultId1.results = new ArrayList<>();
        eventSearchResultId1.results.add(eventSearch1);
        eventSearchResultId1.results.add(eventSearch2);

        when(mockMeetupCalls.askUpcomingEvents("id1")).thenReturn(eventSearchResultId1);

        List<ESCalendarEvents> calendarEventList = meetupApiClient.getUpcomingEvents("id1");

        Assertions.assertThat(calendarEventList).hasSize(2);
        Assertions.assertThat(calendarEventList.get(0).id).matches("meetup_azerty");
        Assertions.assertThat(calendarEventList.get(0).name).matches("Azerty");
        Assertions.assertThat(calendarEventList.get(0).description).matches("AZERTY.");
        Assertions.assertThat(calendarEventList.get(0).date).isEqualTo(123456789000L);
        Assertions.assertThat(calendarEventList.get(0).organizer.name).matches("AzErTy");
        Assertions.assertThat(calendarEventList.get(0).organizer.url).matches("http://www.meetup.com/azerty1");
        Assertions.assertThat(calendarEventList.get(0).location.address).matches("1, Rue Bidon");
        Assertions.assertThat(calendarEventList.get(0).location.city).matches("Ville Bidon");
        Assertions.assertThat(calendarEventList.get(0).location.gps.lat()).isEqualTo(12.3456, within(0.001));
        Assertions.assertThat(calendarEventList.get(0).location.gps.lon()).isCloseTo(34.5678, within(0.001));

        Assertions.assertThat(calendarEventList.get(1).id).matches("meetup_qsdfgh");
        Assertions.assertThat(calendarEventList.get(1).name).matches("Qsdfgh");
        Assertions.assertThat(calendarEventList.get(1).description).matches("QSDFGH.");
        Assertions.assertThat(calendarEventList.get(1).date).isEqualTo(123456987000L);
        Assertions.assertThat(calendarEventList.get(1).organizer.name).isEqualTo("QsDfGh");
        Assertions.assertThat(calendarEventList.get(1).organizer.url).isEqualTo("http://www.meetup.com/qsdfgh1");
    }

    @Test
    public void testMeetupInfo() {
        MeetupInfo meetupInfo = null;

        Group group = new Group();
        group.name = "Group Test";
        group.link = "http://www.group-test.com";
        group.members = 123;
        group.next_event = group.new MeetupApiNextEvent();
        group.next_event.id = "id2";
        Event nextEvent = new Event();
        nextEvent.name = "Event Test";
        nextEvent.event_url = "http://www.event-test.com";
        nextEvent.time = 123456789000L;

        when(mockMeetupCalls.askGroupInfo("id1")).thenReturn(group);
        when(mockMeetupCalls.askEventInfo("id2")).thenReturn(nextEvent);

        try {
            meetupInfo = meetupEndPoint.meetupInfo("id1");
        } catch (IOException e) {
            Assertions.fail("Should not throw an exception !", e);
        }

        Assertions.assertThat(meetupInfo.name).matches("Group Test");
        Assertions.assertThat(meetupInfo.url).matches("http://www.group-test.com");
        Assertions.assertThat(meetupInfo.members).isEqualTo(123);
        Assertions.assertThat(meetupInfo.nextEvent.name).matches("Event Test");
        Assertions.assertThat(meetupInfo.nextEvent.url).matches("http://www.event-test.com");
        Assertions.assertThat(meetupInfo.nextEvent.time).isEqualTo(123456789000L);

        // This should throw an NullPointerException
        try {
            meetupEndPoint.meetupInfo(null);

            Assertions.failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (IOException e) { // Not this one
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            // GOOD !
        }
    }
}
