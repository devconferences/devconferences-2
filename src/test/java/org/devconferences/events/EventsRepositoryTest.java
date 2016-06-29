package org.devconferences.events;

import io.searchbox.indices.Refresh;
import org.assertj.core.api.Assertions;
import org.devconferences.elastic.DeveloppementESNode;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.events.search.CalendarEventSearchResult;
import org.devconferences.events.search.CompletionResult;
import org.devconferences.events.search.EventSearchResult;
import org.devconferences.jobs.ImportEventsJob;
import org.elasticsearch.common.geo.GeoPoint;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.devconferences.elastic.ElasticUtils.DEV_CONFERENCES_INDEX;

public class EventsRepositoryTest {
    private static EventsRepository eventsRepository;
    private static EventsEndPoint eventsEndPoint;

    private static Event event1;
    private static Event event2;
    private static Event event3;
    private static Event event4;
    private static Event event5;
    private static CalendarEvent calendarEvent1;
    private static CalendarEvent calendarEvent2;

    private static RuntimeJestClient client;

    @BeforeClass
    public static void classSetUp() {
        DeveloppementESNode.createDevNode("9250");

        ElasticUtils.createIndex();

        eventsRepository = new EventsRepository();
        eventsEndPoint = new EventsEndPoint(eventsRepository);

        // Add data
        event1 = new Event();
        event1.id = "1";
        event1.name = "awesome";
        event1.description = "an awesome conf";
        event1.url = "http://aweso.me";
        event1.city = "City 1";
        event1.type = Event.Type.CONFERENCE;
        event2 = new Event();
        event2.id = "2";
        event2.name = "Cigale 42";
        event2.city = "City 1";
        event2.type = Event.Type.CONFERENCE;
        event3 = new Event();
        event3.id = "3";
        event3.name = "Event 3";
        event3.city = "City 1";
        event3.type = Event.Type.COMMUNITY;
        event4 = new Event();
        event4.id = "4";
        event4.name = "Event 4";
        event4.city = "City 1";
        event4.type = Event.Type.CONFERENCE;
        event5 = new Event();
        event5.id = "5";
        event5.name = "Event 5";
        event5.city = "City 2";
        event5.type = Event.Type.CONFERENCE;

        eventsRepository.indexOrUpdate(event1);
        eventsRepository.indexOrUpdate(event2);
        eventsRepository.indexOrUpdate(event3);
        eventsRepository.indexOrUpdate(event4);
        eventsRepository.indexOrUpdate(event5);

        calendarEvent1 = new CalendarEvent();
        calendarEvent1.id = "1";
        calendarEvent1.name = "Lorem : 42e ipsum";
        calendarEvent1.description = "an awesome conf";
        calendarEvent1.url = "http://www.example.com";
        calendarEvent1.location = calendarEvent1.new Location();
        calendarEvent1.location.gps = new GeoPoint(44.0500, 5.4500);
        calendarEvent1.date = 2065938828000L;
        calendarEvent2 = new CalendarEvent();
        calendarEvent2.id = "2";
        calendarEvent2.name = "26e Cigale 42";
        calendarEvent2.description = "Event 2";
        calendarEvent2.url = "http://www.example2.com";
        calendarEvent2.location = calendarEvent1.new Location();
        calendarEvent2.location.gps = new GeoPoint(88.0500, -45.4500);
        calendarEvent2.date = 2065938828001L;

        eventsRepository.indexOrUpdate(calendarEvent1);
        eventsRepository.indexOrUpdate(calendarEvent2);

        client = ElasticUtils.createClient();


        // Refresh ES on every document update (assume update are only when start server...)
        // Because users are return with a search, when update a lot of document, some notifications can be lost without this...
        Refresh refresh = new Refresh.Builder().addIndex(DEV_CONFERENCES_INDEX).build();
        client.execute(refresh);
    }

    @AfterClass
    public static void classTearDown() {
        ElasticUtils.deleteAllTypes();

        eventsRepository = null;
        eventsEndPoint = null;
    }

    @Test
    public void testSearchEvent() {
        // Check header content of this searchEvents
        EventSearchResult eventSearch = eventsEndPoint.eventsSearch("awesome", 1, null);
        Assertions.assertThat(eventSearch.hitsAPage).matches("1");
        Assertions.assertThat(eventSearch.totalHits).matches("1");
        Assertions.assertThat(eventSearch.totalPage).matches("1");
        Assertions.assertThat(eventSearch.currPage).matches("1");

        // Should return event passed when created
        List<Event> matches = eventSearch.hits;
        Assertions.assertThat(matches).hasSize(1);
        Assertions.assertThat(matches.get(0)).isEqualTo(event1);

        // With "-1" (and values <= 0), should throw an exception
        try {
            eventSearch = eventsEndPoint.eventsSearch("awesome", -1, null);

            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch(RuntimeException e) {
            // GOOD !
        }
    }

    @Test
    public void testCalendarEventSearch() {
        // Check header content of this searchEvents
        CalendarEventSearchResult eventSearch = eventsEndPoint.eventsCalendarSearch("awesome", 1, null);
        Assertions.assertThat(eventSearch.hitsAPage).matches("1");
        Assertions.assertThat(eventSearch.totalHits).matches("1");
        Assertions.assertThat(eventSearch.totalPage).matches("1");
        Assertions.assertThat(eventSearch.currPage).matches("1");

        // Should return event passed when created
        List<CalendarEvent> matches = eventSearch.hits;
        Assertions.assertThat(matches).hasSize(1);
        Assertions.assertThat(matches.get(0)).isEqualTo(calendarEvent1);
    }

    @Test
    public void testCreateEventWithExistingId() {
        Event event = new Event();
        event.id = "1";
        try {
            eventsEndPoint.createEvent(event); // This : Exception (Event with id "1" already exist ($event1))

            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch(RuntimeException e) {
            // GOOD !
        }
    }

    @Test
    public void testGetEvent() {
        Event event = eventsEndPoint.getEvent("1");
        Assertions.assertThat(event).isEqualTo(event1);
    }

    @Test
    public void testGetCalendarEvent() {
        CalendarEvent calendarEvent = eventsEndPoint.getCalendarEvent("1");
        Assertions.assertThat(calendarEvent).isEqualTo(calendarEvent1);
    }

    @Test
    public void testGetAllCities() {
        List<CityLight> cityLightList = eventsEndPoint.allCities(null);
        Assertions.assertThat(cityLightList).hasSize(2);
        Assertions.assertThat(cityLightList.get(0).count).isEqualTo(4);
        Assertions.assertThat(cityLightList.get(0).name).matches("City 1");
        Assertions.assertThat(cityLightList.get(0).totalCommunity).isEqualTo(1);
        Assertions.assertThat(cityLightList.get(0).totalConference).isEqualTo(3);
    }

    @Test
    public void testGetCity() {
        City city = eventsEndPoint.city("City 1", null);
        Assertions.assertThat(city.id).matches("City 1");
        Assertions.assertThat(city.name).matches("City 1");
        Assertions.assertThat(city.communities).hasSize(1);
        Assertions.assertThat(city.communities.get(0).type).isEqualTo(Event.Type.COMMUNITY);
        Assertions.assertThat(city.conferences).hasSize(3);
        Assertions.assertThat(city.conferences.get(0).type).isEqualTo(Event.Type.CONFERENCE);
        Assertions.assertThat(city.conferences.get(1).type).isEqualTo(Event.Type.CONFERENCE);
        Assertions.assertThat(city.conferences.get(2).type).isEqualTo(Event.Type.CONFERENCE);
        Assertions.assertThat(city.upcoming_events).hasSize(0);

        // With geoSearch of CalendarEvent
        city = eventsEndPoint.city("City 2", null);
        Assertions.assertThat(city.conferences).hasSize(1);
        Assertions.assertThat(city.communities).hasSize(0);
        Assertions.assertThat(city.upcoming_events).hasSize(1);
        Assertions.assertThat(city.upcoming_events.get(0)).isEqualTo(calendarEvent1);
    }

    @Test
    public void testExceptionsWhenDeleteEvent() {
        Event fakeEvent = new Event();
        fakeEvent.id = "666";
        fakeEvent.name = "The number of the beast";
        eventsRepository.indexOrUpdate(fakeEvent);

        try {
            eventsRepository.deleteEvent(null);

            Assertions.failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch(NullPointerException e) {
            // GOOD !
        }
        try {
            eventsRepository.deleteEvent("");

            Assertions.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch(IllegalArgumentException e) {
            // GOOD !
        }
        try {
            eventsRepository.deleteEvent("666"); // Should pass
        } catch(RuntimeException e) {
            Assertions.fail("Should not throw an exception !", e);
        }
    }

    @Test
    public void testGetCalendarEventsWithPagination() {
        List<CalendarEvent> calendarEventList = eventsEndPoint.getCalendarEvents("10");
        Assertions.assertThat(calendarEventList).hasSize(2);
        Assertions.assertThat(calendarEventList.get(0)).isEqualTo(calendarEvent1);
        Assertions.assertThat(calendarEventList.get(1)).isEqualTo(calendarEvent2);
    }

    @Test
    public void testGetSuggests() {
        CompletionResult suggestDatas = eventsEndPoint.suggest("Ci", null);
        Assertions.assertThat(suggestDatas.hits).hasSize(3);
        Assertions.assertThat(suggestDatas.hits.get(0).text).matches("Cigale");
    }

    @Test
    public void testEventGeoSearch() {
        Event event = new Event();
        event.id = UUID.randomUUID().toString();
        event.gps = new GeoPoint(1.0f, 1.0f);

        Event event2 = new Event();
        event2.id = UUID.randomUUID().toString();
        event2.gps = new GeoPoint(50.0f, 50.0f);

        EventsRepository eventsRepository = new EventsRepository();
        eventsRepository.createEvent(event);
        eventsRepository.createEvent(event2);

        // Refresh ES on every document update (assume update are only when start server...)
        // Because users are return with a search, when update a lot of document, some notifications can be lost without this...
        Refresh refresh = new Refresh.Builder().addIndex(DEV_CONFERENCES_INDEX).build();
        client.execute(refresh);

        // This should find $event, and not $event2
        List<Event> events = eventsRepository.findEventsAround(1.0f, 1.0f, 10);
        Assertions.assertThat(events).hasSize(1);
        Assertions.assertThat(events.get(0).gps).isEqualToComparingFieldByField(new GeoPoint(1.0f, 1.0f));

        eventsRepository.deleteEvent(event.id);
        eventsRepository.deleteEvent(event2.id);

        client.execute(refresh);
    }

    @Test
    public void testGeopointCities() {
        new ImportEventsJob().reloadData(true);

        GeopointCities geopointCities = GeopointCities.getInstance();
        Assertions.assertThat(geopointCities).isNotNull();

        Assertions.assertThat(geopointCities.getLocation("the_city"))
                .isEqualToComparingFieldByField(new GeoPoint("49.900,2.3000"));
        Assertions.assertThat(geopointCities.getLocation("the_city2"))
                .isEqualToComparingFieldByField(new GeoPoint("47.4800,-0.5400"));
        Assertions.assertThat(geopointCities.getLocation("the_city3")).isNull();

        // TearDown
        eventsRepository.deleteEvent("testevent");
        eventsRepository.deleteEvent("testevent2");

        Refresh refresh = new Refresh.Builder().addIndex(DEV_CONFERENCES_INDEX).build();
        client.execute(refresh);
    }
}