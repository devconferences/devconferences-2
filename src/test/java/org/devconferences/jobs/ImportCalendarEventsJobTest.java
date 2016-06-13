package org.devconferences.jobs;

import io.searchbox.core.Delete;
import org.assertj.core.api.Assertions;
import org.devconferences.elastic.DeveloppementESNode;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.MockJestClient;
import org.devconferences.elastic.RuntimeJestClientAdapter;
import org.devconferences.events.CalendarEvent;
import org.devconferences.events.ESCalendarEvents;
import org.devconferences.meetup.MeetupApiClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by ronan on 18/05/16.
 */
public class ImportCalendarEventsJobTest {
    private ImportCalendarEventsJob importCalendarEventsJob;
    private RuntimeJestClientAdapter mockJestClient;
    private MeetupApiClient mockMeetupClient;

    @BeforeClass
    public static void setUpOnce() {
        DeveloppementESNode.setPortNode("0");
    }

    @Before
    public void setUp() {
        mockJestClient = MockJestClient.createMock();
        mockMeetupClient = mock(MeetupApiClient.class);
        importCalendarEventsJob = new ImportCalendarEventsJob(mockJestClient, mockMeetupClient);
    }

    @Test
    public void testReloadData() {
        when(mockJestClient.execute(isA(Delete.class))).thenReturn(null);
        int totalImportedFiles = importCalendarEventsJob.reloadData(true);
        Assertions.assertThat(totalImportedFiles).isEqualTo(0); // 1 file ignored
    }

    @Test
    public void testMeetupIdsList() {
        when(mockJestClient.execute(isA(Delete.class))).thenReturn(null);

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
        when(mockJestClient.execute(isA(Delete.class))).thenReturn(null);

        ESCalendarEvents calendarEvent1 = new ESCalendarEvents();
        ESCalendarEvents calendarEvent2 = new ESCalendarEvents();
        ESCalendarEvents calendarEvent3 = new ESCalendarEvents();
        ArrayList<ESCalendarEvents> calendarEventAaaa = new ArrayList<>();
        calendarEventAaaa.add(calendarEvent1);
        ArrayList<ESCalendarEvents> calendarEventBbbb = new ArrayList<>();
        calendarEventBbbb.add(calendarEvent2);
        calendarEventBbbb.add(calendarEvent3);
        when(mockMeetupClient.getUpcomingEvents("aaaa")).thenReturn(calendarEventAaaa);
        when(mockMeetupClient.getUpcomingEvents("bbbb")).thenReturn(calendarEventBbbb);

        ImportCalendarEventsJob.idMeetupList.clear();
        ImportCalendarEventsJob.addIdMeetup("aaaa");
        ImportCalendarEventsJob.addIdMeetup("bbbb");

        int totalImportedEvents = importCalendarEventsJob.reloadData(false);
        Assertions.assertThat(totalImportedEvents).isEqualTo(3); // 0 from files (1 ignored), 3 from Meetup mock
    }

    @Test
    public void testCalendarCheck() {
        CalendarEvent calendarEvent = new CalendarEvent();
        try {
            ImportCalendarEventsJob.checkCalendarEvent(calendarEvent, "/calendar/4sqcd/05/file_test.json");
            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch(RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid CalendarEvent : year in path is NaN");
        }
        try {
            ImportCalendarEventsJob.checkCalendarEvent(calendarEvent, "/calendar/2016/xx/file_test.json");
            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch(RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid CalendarEvent : month in path is NaN");
        }

        try {
            ImportCalendarEventsJob.checkCalendarEvent(calendarEvent, "/calendar/2016/05/test.json");
            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch(RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid CalendarEvent : no 'id' field");
        }
        try {
            calendarEvent.id = "TEST";
            ImportCalendarEventsJob.checkCalendarEvent(calendarEvent, "/calendar/2016/05/test.json");
            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch(RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid CalendarEvent : no 'name' field");
        }

        try {
            calendarEvent.name = "Test";
            ImportCalendarEventsJob.checkCalendarEvent(calendarEvent, "/calendar/2016/05/test.json");
            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch(RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid CalendarEvent : no 'date' field");
        }
        try {
            calendarEvent.date = 123456789000L;
            ImportCalendarEventsJob.checkCalendarEvent(calendarEvent, "/calendar/2016/05/test.json");
            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch(RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid CalendarEvent : no 'description' field");
        }
        try {
            calendarEvent.description = "Test sur ImportCalendarEventsJobTest";
            ImportCalendarEventsJob.checkCalendarEvent(calendarEvent, "/calendar/2016/05/test.json");
            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch(RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid CalendarEvent : filename and 'id' field mismatch");
        }
        try {
            calendarEvent.id = "test";
            ImportCalendarEventsJob.checkCalendarEvent(calendarEvent, "/calendar/2016/05/test.json");
            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch(RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid CalendarEvent : year path and 'date' field mismatch\n" +
                    "date year:1973\n" +
                    "path year:2016");
        }
        try {
            calendarEvent.date = 1474567890000L;
            ImportCalendarEventsJob.checkCalendarEvent(calendarEvent, "/calendar/2016/05/test.json");
            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch(RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid CalendarEvent : month path and 'date' field mismatch\n" +
                    "date month:9\n" +
                    "path month:5");
        }
        try {
            calendarEvent.date = 1462567890000L;
            ImportCalendarEventsJob.checkCalendarEvent(calendarEvent, "/calendar/2016/05/test.json");
        } catch(RuntimeException e) {
            Assertions.fail("Should not throw an exception !");
        }
    }

    @Test
    public void testCheckAllData() {
        try {
            importCalendarEventsJob.checkAllData();
        } catch(RuntimeException e) {
            Assertions.fail("Should not throw an exception !", e);
        }
    }

    @Test
    public void testCheckDataFail() {
        try {
            importCalendarEventsJob.checkData("/calendar_fail/2016/04/file_kejfnffsdf.json");
        } catch(RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid CalendarEvent : " +
                    "no 'description' field - file path : /calendar_fail/2016/04/file_kejfnffsdf.json");
        }
    }
}
