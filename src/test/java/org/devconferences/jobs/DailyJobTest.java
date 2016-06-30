package org.devconferences.jobs;

import com.google.gson.Gson;
import io.searchbox.core.Count;
import io.searchbox.core.CountResult;
import io.searchbox.indices.Refresh;
import org.assertj.core.api.Assertions;
import org.devconferences.elastic.DeveloppementESNode;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.events.data.CalendarEvent;
import org.devconferences.events.data.Event;
import org.devconferences.events.EventsRepository;
import org.devconferences.meetup.MeetupApiClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStreamReader;
import java.util.ArrayList;

import static org.devconferences.elastic.ElasticUtils.DEV_CONFERENCES_INDEX;
import static org.devconferences.jobs.ImportCalendarEventsJob.CALENDAREVENTS_TYPE;
import static org.devconferences.jobs.ImportEventsJob.EVENTS_TYPE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DailyJobTest {
    private static ImportCalendarEventsJob importCalendarEventsJob;
    private static ImportEventsJob importEventsJob;
    private static DailyJob dailyJob;
    private static RuntimeJestClient client;
    private static MeetupApiClient mockMeetupClient;
    private static CalendarEvent calendarEvent1;
    private static CalendarEvent calendarEvent2;
    private static CalendarEvent calendarEvent3;
    private static CalendarEvent calendarEvent4;
    private static Event event1;


    @BeforeClass
    public static void classSetUp() {
        DeveloppementESNode.createDevNode("9250");
        ElasticUtils.createIndex();

        client = ElasticUtils.createClient();

        mockMeetupClient = mock(MeetupApiClient.class);
        importCalendarEventsJob = new ImportCalendarEventsJob(client, mockMeetupClient);
        importEventsJob = new ImportEventsJob(client);
        dailyJob = new DailyJob(importCalendarEventsJob);

        // Add data to mock
        calendarEvent1 = new CalendarEvent();
        calendarEvent1.id = "1562462143";
        calendarEvent1.date = 2065938828000L;
        calendarEvent1.name = "Event 1";
        calendarEvent2 = new CalendarEvent();
        calendarEvent2.id = "1562462144";
        calendarEvent2.date = 2065938828001L;
        calendarEvent2.name = "Event 2";
        calendarEvent3 = new CalendarEvent();
        calendarEvent3.id = "1562462145";
        calendarEvent3.date = 123456786000L;
        calendarEvent3.name = "Event 3";
        ArrayList<CalendarEvent> calendarEventAaaa = new ArrayList<>();
        calendarEventAaaa.add(calendarEvent1);
        ArrayList<CalendarEvent> calendarEventBbbb = new ArrayList<>();
        calendarEventBbbb.add(calendarEvent2);
        calendarEventBbbb.add(calendarEvent3);
        when(mockMeetupClient.getUpcomingEvents("aaaa")).thenReturn(calendarEventAaaa);
        when(mockMeetupClient.getUpcomingEvents("bbbb")).thenReturn(calendarEventBbbb);

        calendarEvent4 = new Gson().fromJson(
                new InputStreamReader(AbstractImportJSONJob.class.getResourceAsStream("/calendar/2035/06/kejfnffsdfaa.json"))
                , CalendarEvent.class);
        event1 = new Gson().fromJson(
                new InputStreamReader(AbstractImportJSONJob.class.getResourceAsStream("/events/the_city2/testeventhidden.json"))
                , Event.class);

        ImportCalendarEventsJob.idMeetupList.clear();
        ImportCalendarEventsJob.addIdMeetup("aaaa");
        ImportCalendarEventsJob.addIdMeetup("bbbb");
    }

    @AfterClass
    public static void tearDownOne() {
        ElasticUtils.deleteAllTypes();
    }

    @Before
    public void setUp() {
        importEventsJob.reloadData(false);
        importCalendarEventsJob.reloadData(false);

        Refresh refresh = new Refresh.Builder().addIndex(DEV_CONFERENCES_INDEX).build();
        client.execute(refresh);
    }

    @Test
    public void testEachDailyJob() {
        EventsRepository eventsRepository = new EventsRepository();
        Refresh refresh = new Refresh.Builder().addIndex(DEV_CONFERENCES_INDEX).build();

        //********** CalendarEvent **********//

        Count count = new Count.Builder().addIndex(DEV_CONFERENCES_INDEX).addType(CALENDAREVENTS_TYPE).build();
        CountResult countResult = client.execute(count);
        Assertions.assertThat(countResult.getCount().intValue()).isEqualTo(3);

        // First : index this upcoming event
        calendarEvent4.hidden = false;
        eventsRepository.indexOrUpdate(calendarEvent4);

        client.execute(refresh);

        countResult = client.execute(count);
        Assertions.assertThat(countResult.getCount().intValue()).isEqualTo(4);

        // Second : Re-index with hidden = true;
        calendarEvent4.hidden = true;
        eventsRepository.indexOrUpdate(calendarEvent4);

        client.execute(refresh);

        countResult = client.execute(count);
        Assertions.assertThat(countResult.getCount().intValue()).isEqualTo(4);

        // Third : remove hidden CalendarEvents, which should remove calendarEvent4
        dailyJob.removeHiddenCalendarEvents(client);

        client.execute(refresh);

        countResult = client.execute(count);
        Assertions.assertThat(countResult.getCount().intValue()).isEqualTo(3);

        // Fourth : remove old CalendarEvents, which should remvoe calendarEvent3
        dailyJob.removeOldCalendarEvents(client);

        client.execute(refresh);

        countResult = client.execute(count);
        Assertions.assertThat(countResult.getCount().intValue()).isEqualTo(2);

        //********** Event **********//

        count = new Count.Builder().addIndex(DEV_CONFERENCES_INDEX).addType(EVENTS_TYPE).build();
        countResult = client.execute(count);
        Assertions.assertThat(countResult.getCount().intValue()).isEqualTo(2);

        // First : index this event
        event1.hidden = false;
        eventsRepository.indexOrUpdate(event1);

        client.execute(refresh);

        countResult = client.execute(count);
        Assertions.assertThat(countResult.getCount().intValue()).isEqualTo(3);

        // Second : Re-index with hidden = true;
        event1.hidden = true;
        eventsRepository.indexOrUpdate(event1);

        client.execute(refresh);

        countResult = client.execute(count);
        Assertions.assertThat(countResult.getCount().intValue()).isEqualTo(3);

        // Third : remove hidden CalendarEvents, which should remove event1
        dailyJob.removeHiddenEvents(client);

        client.execute(refresh);

        countResult = client.execute(count);
        Assertions.assertThat(countResult.getCount().intValue()).isEqualTo(2);
    }

    @Test
    public void testAllDailyJob() {
        EventsRepository eventsRepository = new EventsRepository();
        Refresh refresh = new Refresh.Builder().addIndex(DEV_CONFERENCES_INDEX).build();
        Count countCalendarEvent = new Count.Builder().addIndex(DEV_CONFERENCES_INDEX).addType(CALENDAREVENTS_TYPE).build();
        Count countEvent = new Count.Builder().addIndex(DEV_CONFERENCES_INDEX).addType(EVENTS_TYPE).build();
        CountResult countCalendarEventResult;
        CountResult countEventResult;

        // Initial assertions
        countCalendarEventResult = client.execute(countCalendarEvent);
        Assertions.assertThat(countCalendarEventResult.getCount().intValue()).isEqualTo(3);
        countEventResult = client.execute(countEvent);
        Assertions.assertThat(countEventResult.getCount().intValue()).isEqualTo(2);

        // First : index all
        calendarEvent4.hidden = false;
        event1.hidden = false;
        eventsRepository.indexOrUpdate(calendarEvent4);
        eventsRepository.indexOrUpdate(event1);

        client.execute(refresh);

        countCalendarEventResult = client.execute(countCalendarEvent);
        Assertions.assertThat(countCalendarEventResult.getCount().intValue()).isEqualTo(4);
        countEventResult = client.execute(countEvent);
        Assertions.assertThat(countEventResult.getCount().intValue()).isEqualTo(3);

        // Second : Re-index with hidden = true;
        calendarEvent4.hidden = true;
        event1.hidden = true;
        eventsRepository.indexOrUpdate(calendarEvent4);
        eventsRepository.indexOrUpdate(event1);

        client.execute(refresh);

        countCalendarEventResult = client.execute(countCalendarEvent);
        Assertions.assertThat(countCalendarEventResult.getCount().intValue()).isEqualTo(4);
        countEventResult = client.execute(countEvent);
        Assertions.assertThat(countEventResult.getCount().intValue()).isEqualTo(3);

        // Third : doDailyJob, which should remove event1, calendarEvent3 and calendarEvent4
        dailyJob.doJob();

        client.execute(refresh);

        countCalendarEventResult = client.execute(countCalendarEvent);
        Assertions.assertThat(countCalendarEventResult.getCount().intValue()).isEqualTo(2);
        countEventResult = client.execute(countEvent);
        Assertions.assertThat(countEventResult.getCount().intValue()).isEqualTo(2);
    }
}
