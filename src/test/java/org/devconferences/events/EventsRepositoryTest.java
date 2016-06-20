package org.devconferences.events;

import io.searchbox.indices.Refresh;
import org.assertj.core.api.Assertions;
import org.devconferences.elastic.*;
import org.devconferences.events.search.CalendarEventSearch;
import org.devconferences.events.search.EventSearch;
import org.devconferences.events.search.CompletionSearch;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.UUID;

import static org.devconferences.elastic.ElasticUtils.DEV_CONFERENCES_INDEX;

public class EventsRepositoryTest {
    private static EventsRepository eventsRepository;
    private static EventsEndPoint eventsEndPoint;

    @BeforeClass
    public static void classSetUp() {
        DeveloppementESNode.createDevNode("9250");

        ElasticUtils.createIndex();

        eventsRepository = new EventsRepository();
        eventsEndPoint = new EventsEndPoint(eventsRepository);

        // Add data
        Event event1 = new Event();
        event1.id = "1";
        event1.name = "awesome";
        event1.description = "an awesome conf";
        event1.city = "City 1";
        event1.type = Event.Type.CONFERENCE;
        Event event2 = new Event();
        event2.id = "2";
        event2.name = "Cigale 42";
        event2.city = "City 1";
        event2.type = Event.Type.CONFERENCE;
        Event event3 = new Event();
        event3.id = "3";
        event3.name = "Event 3";
        event3.city = "City 1";
        event3.type = Event.Type.COMMUNITY;
        Event event4 = new Event();
        event4.id = "4";
        event4.name = "Event 4";
        event4.city = "City 1";
        event4.type = Event.Type.CONFERENCE;
        Event event5 = new Event();
        event5.id = "5";
        event5.name = "Event 5";
        event5.city = "City 2";
        event5.type = Event.Type.CONFERENCE;

        eventsRepository.indexOrUpdate(event1);
        eventsRepository.indexOrUpdate(event2);
        eventsRepository.indexOrUpdate(event3);
        eventsRepository.indexOrUpdate(event4);
        eventsRepository.indexOrUpdate(event5);

        CalendarEvent calendarEvent1 = new CalendarEvent();
        calendarEvent1.id = "1";
        calendarEvent1.name = "Lorem : 42e ipsum";
        calendarEvent1.description = "an awesome conf";
        calendarEvent1.url = "http://www.example.com";
        calendarEvent1.date = 2065938828000L;
        CalendarEvent calendarEvent2 = new CalendarEvent();
        calendarEvent2.id = "2";
        calendarEvent2.name = "26e Cigale 42";
        calendarEvent2.description = "Event 2";
        calendarEvent2.url = "http://www.example2.com";
        calendarEvent2.date = 2065938828000L;

        eventsRepository.indexOrUpdate(calendarEvent1);
        eventsRepository.indexOrUpdate(calendarEvent2);


        // Refresh ES on every document update (assume update are only when start server...)
        // Because users are return with a search, when update a lot of document, some notifications can be lost without this...
        Refresh refresh = new Refresh.Builder().addIndex(DEV_CONFERENCES_INDEX).build();
        ElasticUtils.createClient().execute(refresh);
    }

    @AfterClass
    public static void tearDownOne() {
        ElasticUtils.deleteIndex();

        eventsRepository = null;
        eventsEndPoint = null;
    }

    @Test
    public void should_find_event() {
        // Check header content of this searchEvents
        EventSearch eventSearch = eventsEndPoint.eventsSearch("awesome", "1", null);
        Assertions.assertThat(eventSearch.hitsAPage).matches("1");
        Assertions.assertThat(eventSearch.totalHits).matches("1");
        Assertions.assertThat(eventSearch.totalPage).matches("1");
        Assertions.assertThat(eventSearch.currPage).matches("1");

        // Should return event passed when created
        List<Event> matches = eventSearch.hits;
        Assertions.assertThat(matches).hasSize(1);
        Assertions.assertThat(matches.get(0).id).matches("1");
        Assertions.assertThat(matches.get(0).city).matches("City 1");
        Assertions.assertThat(matches.get(0).description).matches("an awesome conf");

        // With "-1" (and values <= 0), should throw an exception
        try {
            eventSearch = eventsEndPoint.eventsSearch("awesome", "-1", null);

            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch (RuntimeException e) {
            // GOOD !
        }
    }

    @Test
    public void should_find_calendar_event() {
        // Check header content of this searchEvents
        CalendarEventSearch eventSearch = eventsEndPoint.eventsCalendarSearch("awesome", "1", null);
        Assertions.assertThat(eventSearch.hitsAPage).matches("1");
        Assertions.assertThat(eventSearch.totalHits).matches("1");
        Assertions.assertThat(eventSearch.totalPage).matches("1");
        Assertions.assertThat(eventSearch.currPage).matches("1");

        // Should return event passed when created
        List<CalendarEvent> matches = eventSearch.hits;
        Assertions.assertThat(matches).hasSize(1);
        Assertions.assertThat(matches.get(0).id).matches("1");
        Assertions.assertThat(matches.get(0).date).isEqualTo(2065938828000L);
        Assertions.assertThat(matches.get(0).name).matches("Lorem : 42e ipsum");
        Assertions.assertThat(matches.get(0).description).matches("an awesome conf");
    }

    @Test
    public void should_throw_exception_when_create_event_with_existing_id() {
        Event event = new Event();
        event.id = "1";
        try {
            eventsEndPoint.createEvent(event); // This : Exception

            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch(RuntimeException e) {
            // GOOD !
        }
    }

    @Test
    public void should_get_event_with_an_id() {
        Event event2 = eventsEndPoint.getEvent("1");
        Assertions.assertThat(event2.id).matches("1");
        Assertions.assertThat(event2.name).matches("awesome");
        Assertions.assertThat(event2.description).matches("an awesome conf");
        Assertions.assertThat(event2.city).matches("City 1");
        Assertions.assertThat(event2.type).isEqualTo(Event.Type.CONFERENCE);
    }

    @Test
    public void should_find_cities() {
        List<CityLight> cityLightList = eventsEndPoint.allCities(null);
        Assertions.assertThat(cityLightList).hasSize(2);
        Assertions.assertThat(cityLightList.get(0).count).isEqualTo(4);
        Assertions.assertThat(cityLightList.get(0).name).matches("City 1");
        Assertions.assertThat(cityLightList.get(0).totalCommunity).isEqualTo(1);
        Assertions.assertThat(cityLightList.get(0).totalConference).isEqualTo(3);
    }

    @Test
    public void should_find_a_city() {
        City city = eventsEndPoint.city("City 1", null);
        Assertions.assertThat(city.id).matches("City 1");
        Assertions.assertThat(city.name).matches("City 1");
        Assertions.assertThat(city.communities).hasSize(1);
        Assertions.assertThat(city.communities.get(0).type).isEqualTo(Event.Type.COMMUNITY);
        Assertions.assertThat(city.conferences).hasSize(3);
        Assertions.assertThat(city.conferences.get(0).type).isEqualTo(Event.Type.CONFERENCE);
        Assertions.assertThat(city.conferences.get(1).type).isEqualTo(Event.Type.CONFERENCE);
        Assertions.assertThat(city.conferences.get(2).type).isEqualTo(Event.Type.CONFERENCE);
    }

    @Test
    public void should_detect_empty_ids_when_delete() {
        Event fakeEvent = new Event();
        fakeEvent.id = "666";
        fakeEvent.name = "The number of the beast";
        eventsRepository.indexOrUpdate(fakeEvent);

        try {
            eventsRepository.deleteEvent(null);

            Assertions.failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
            // GOOD !
        }
        try {
            eventsRepository.deleteEvent("");

            Assertions.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            // GOOD !
        }
        try {
            eventsRepository.deleteEvent("666"); // Should pass
        } catch (RuntimeException e) {
            Assertions.fail("Should not throw an exception !", e);
        }
    }

    @Test
    public void should_find_calendar_events() {
        List<CalendarEvent> calendarEventList = eventsEndPoint.getCalendarEvents("10");
        Assertions.assertThat(calendarEventList).hasSize(2);
        Assertions.assertThat(calendarEventList.get(0).id).matches("1");
        Assertions.assertThat(calendarEventList.get(0).name).matches("Lorem : 42e ipsum");
        Assertions.assertThat(calendarEventList.get(0).description).matches("an awesome conf");
        Assertions.assertThat(calendarEventList.get(0).url).matches("http://www.example.com");
    }

    @Test
    public void should_find_suggestions() {
        CompletionSearch suggestDatas = eventsEndPoint.suggest("Ci", null);
        Assertions.assertThat(suggestDatas.hits).hasSize(3);
        Assertions.assertThat(suggestDatas.hits.get(0).text).matches("Cigale");
        Assertions.assertThat(suggestDatas.hits.get(0).score).isEqualTo(2.0d);
    }
}