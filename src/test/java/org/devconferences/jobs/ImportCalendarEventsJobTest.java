package org.devconferences.jobs;

import org.assertj.core.api.Assertions;
import org.devconferences.elastic.MockJestClient;
import org.devconferences.elastic.RuntimeJestClientAdapter;
import org.devconferences.events.CalendarEvent;
import org.devconferences.events.EventsRepository;
import org.devconferences.meetup.MeetupApiClient;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by ronan on 18/05/16.
 */
public class ImportCalendarEventsJobTest {
    private ImportCalendarEventsJob importCalendarEventsJob;
    private RuntimeJestClientAdapter mockJestClient;
    private MeetupApiClient mockMeetupClient;

    @Before
    public void setUp() {
        mockJestClient = MockJestClient.createMock(EventsRepository.EVENTS_TYPE);
        mockMeetupClient = mock(MeetupApiClient.class);
        importCalendarEventsJob = new ImportCalendarEventsJob(mockJestClient, mockMeetupClient);
    }

    @Test
    public void testReloadData() {
        int totalImportedFiles = importCalendarEventsJob.reloadData(true);
        Assertions.assertThat(totalImportedFiles).isEqualTo(1);
    }

    @Test
    public void testMeetupIdsList() {
        // When reload Events
        ImportCalendarEventsJob.idMeetupList.clear();

        ImportEventsJob importEventsJob = new ImportEventsJob(mockJestClient);
        importEventsJob.reloadData(true);
        importCalendarEventsJob.reloadData(true);

        Assertions.assertThat(ImportCalendarEventsJob.idMeetupList).hasSize(1);
        Assertions.assertThat(ImportCalendarEventsJob.idMeetupList.contains("lskfpfs1265")).isTrue();

        // When read file .meetupIdList
        ImportCalendarEventsJob.idMeetupList.clear();
        ImportCalendarEventsJob.reloadMeetupIds();

        Assertions.assertThat(ImportCalendarEventsJob.idMeetupList).hasSize(1);
        Assertions.assertThat(ImportCalendarEventsJob.idMeetupList.contains("lskfpfs1265")).isTrue();
    }

    @Test
    public void testMeetupImport() {
        CalendarEvent calendarEvent1 = new CalendarEvent();
        CalendarEvent calendarEvent2 = new CalendarEvent();
        CalendarEvent calendarEvent3 = new CalendarEvent();
        ArrayList<CalendarEvent> calendarEventAaaa = new ArrayList<>();
        calendarEventAaaa.add(calendarEvent1);
        ArrayList<CalendarEvent> calendarEventBbbb = new ArrayList<>();
        calendarEventBbbb.add(calendarEvent2);
        calendarEventBbbb.add(calendarEvent3);
        when(mockMeetupClient.getUpcomingEvents("aaaa")).thenReturn(calendarEventAaaa);
        when(mockMeetupClient.getUpcomingEvents("bbbb")).thenReturn(calendarEventBbbb);

        ImportCalendarEventsJob.idMeetupList.clear();
        ImportCalendarEventsJob.addIdMeetup("aaaa");
        ImportCalendarEventsJob.addIdMeetup("bbbb");

        int totalImportedEvents = importCalendarEventsJob.reloadData(false);
        Assertions.assertThat(totalImportedEvents).isEqualTo(4); // 1 from file, 3 from Meetup mock
    }
}
