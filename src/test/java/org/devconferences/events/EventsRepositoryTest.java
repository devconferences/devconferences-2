package org.devconferences.events;

import net.codestory.http.Context;
import org.assertj.core.api.Assertions;
import org.devconferences.elastic.MockJestClient;
import org.devconferences.elastic.RuntimeJestClientAdapter;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

public class EventsRepositoryTest {
    private RuntimeJestClientAdapter mockClient;
    private EventsRepository eventsRepository;
    private EventsEndPoint eventsEndPoint;

    @Before
    public void setUp() {
        mockClient = MockJestClient.createMock(EventsRepository.EVENTS_TYPE);
        eventsRepository = new EventsRepository(mockClient);
        eventsEndPoint = new EventsEndPoint(eventsRepository);
    }

    @Test
    public void should_find_event() {
        Event event = new Event();
        event.id = UUID.randomUUID().toString();
        event.city = "the_city";
        event.description = "an awesome conf";

        int countCount = 1;
        MockJestClient.configCount(mockClient, EventsRepository.EVENTS_TYPE, countCount);

        int countSearch = 1;
        String searchHits = "[" +
                "  {" +
                "    \"_index\" : \"dev-conferences\"," +
                "    \"_type\" : \"events\"," +
                "    \"_id\" : \"" + event.id + "\"," +
                "    \"_source\" : {" +
                "      \"id\" : \"" + event.id + "\"," +
                "      \"city\" : \"" + event.city + "\"," +
                "      \"description\" : \"" + event.description + "\"" +
                "    }" +
                "  }" +
                "]";
        MockJestClient.configSearch(mockClient, EventsRepository.EVENTS_TYPE, countSearch, searchHits, "{}");

        String jsonGet = "{" +
                "  \"_index\": \"dev-conferences\" ," +
                "  \"_type\": \"events\" ," +
                "  \"_id\": \"1\" ," +
                "  \"_version\": 1 ," +
                "  \"found\": false ," +
                "  \"_source\": null" +
                "}";
        MockJestClient.configGet(mockClient, EventsRepository.EVENTS_TYPE, jsonGet);

        eventsEndPoint.createEvent(event);

        // Check header content of this searchEvents
        AbstractSearchResult eventSearch = eventsEndPoint.eventsSearch("awesome", "1", null, null, null, null);
        Assertions.assertThat(eventSearch.hitsAPage).matches("10");
        Assertions.assertThat(eventSearch.totalHits).matches("1");
        Assertions.assertThat(eventSearch.totalPage).matches("1");
        Assertions.assertThat(eventSearch.currPage).matches("1");

        // Should return event passed when created
        List<Event> matches = eventSearch.hits;
        Assertions.assertThat(matches).hasSize(1);
        Assertions.assertThat(matches.get(0).id).matches(event.id);
        Assertions.assertThat(matches.get(0).city).matches(event.city);
        Assertions.assertThat(matches.get(0).description).matches(event.description);

        // With "0", should show all hits
        eventSearch = eventsEndPoint.eventsSearch("awesome", "0", null, null, null, null);
        Assertions.assertThat(eventSearch.hitsAPage).matches("1");
        Assertions.assertThat(eventSearch.totalHits).matches("1");
        Assertions.assertThat(eventSearch.totalPage).matches("0");
        Assertions.assertThat(eventSearch.currPage).matches("0");
        Assertions.assertThat(eventSearch.hits).hasSize(1);

        // With "-1" (and values < 0), should throw an exception
        try {
            eventSearch = eventsEndPoint.eventsSearch("awesome", "-1", null, null, null, null);

            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch (RuntimeException e) {
            // GOOD !
        }
    }

    @Test
    public void should_find_calendar_event() {
        CalendarEvent event = new CalendarEvent();
        event.id = UUID.randomUUID().toString();
        event.date = 123456789000L;
        event.name = "a  conf";
        event.description = "an awesome conf";

        int countCount = 1;
        MockJestClient.configCount(mockClient, EventsRepository.CALENDAREVENTS_TYPE, countCount);

        int countSearch = 1;
        String searchHits = "[" +
                "  {" +
                "    \"_index\" : \"dev-conferences\"," +
                "    \"_type\" : \"events\"," +
                "    \"_id\" : \"" + event.id + "\"," +
                "    \"_source\" : {" +
                "      \"id\" : \"" + event.id + "\"," +
                "      \"date\" : \"" + event.date + "\"," +
                "      \"name\" : \"" + event.name + "\"," +
                "      \"description\" : \"" + event.description + "\"" +
                "    }" +
                "  }" +
                "]";
        MockJestClient.configSearch(mockClient, EventsRepository.CALENDAREVENTS_TYPE, countSearch, searchHits, "{}");

        String jsonGet = "{" +
                "  \"_index\": \"dev-conferences\" ," +
                "  \"_type\": \"events\" ," +
                "  \"_id\": \"1\" ," +
                "  \"_version\": 1 ," +
                "  \"found\": false ," +
                "  \"_source\": null" +
                "}";
        MockJestClient.configGet(mockClient, EventsRepository.CALENDAREVENTS_TYPE, jsonGet);

        eventsRepository.indexOrUpdate(event);

        // Check header content of this searchEvents
        AbstractSearchResult eventSearch = eventsEndPoint.eventsCalendarSearch("awesome", "1", null, null, null, null);
        Assertions.assertThat(eventSearch.hitsAPage).matches("10");
        Assertions.assertThat(eventSearch.totalHits).matches("1");
        Assertions.assertThat(eventSearch.totalPage).matches("1");
        Assertions.assertThat(eventSearch.currPage).matches("1");

        // Should return event passed when created
        List<CalendarEvent> matches = eventSearch.hits;
        Assertions.assertThat(matches).hasSize(1);
        Assertions.assertThat(matches.get(0).id).matches(event.id);
        Assertions.assertThat(matches.get(0).date).isEqualTo(event.date);
        Assertions.assertThat(matches.get(0).name).matches(event.name);
        Assertions.assertThat(matches.get(0).description).matches(event.description);
    }

    @Test
    public void should_throw_exception_when_create_event_with_existing_id() {
        // Create mock
        String jsonGet = "{" +
                "  \"_index\": \"dev-conferences\" ," +
                "  \"_type\": \"events\" ," +
                "  \"_id\": \"1\" ," +
                "  \"_version\": 1 ," +
                "  \"found\": true ," +
                "  \"_source\": {" +
                "    \"id\": \"1\" ," +
                "    \"name\": \"Event Test\" ," +
                "    \"city\": \"the_city\" ," +
                "    \"type\": \"CONFERENCE\"" +
                "  }" +
                "}";
        MockJestClient.configGet(mockClient, EventsRepository.EVENTS_TYPE, jsonGet);

        Event event = new Event();
        event.id = "1";
        try {
            eventsEndPoint.createEvent(event);

            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch(RuntimeException e) {
            // GOOD !
        }
    }

    @Test
    public void should_get_event_with_an_id() {
        // Create mock
        String jsonGet = "{" +
                "  \"_index\": \"dev-conferences\" ," +
                "  \"_type\": \"events\" ," +
                "  \"_id\": \"1\" ," +
                "  \"_version\": 1 ," +
                "  \"found\": true ," +
                "  \"_source\": {" +
                "    \"id\": \"1\" ," +
                "    \"name\": \"Event Test\" ," +
                "    \"city\": \"the_city\" ," +
                "    \"type\": \"CONFERENCE\"" +
                "  }" +
                "}";
        MockJestClient.configGet(mockClient, EventsRepository.EVENTS_TYPE, jsonGet);

        Event event = new Event();
        event.id = "1";
        Event event2 = eventsEndPoint.getEvent("1");
        Assertions.assertThat(event2.id).matches("1");
        Assertions.assertThat(event2.name).matches("Event Test");
        Assertions.assertThat(event2.city).matches("the_city");
        Assertions.assertThat(event2.type).isEqualTo(Event.Type.CONFERENCE);
    }

    @Test
    public void should_find_cities() {
        String searchAggreg = "{" +
                "  \"cities\": {" +
                "    \"doc_count_error_upper_bound\": 0 ," +
                "    \"sum_other_doc_count\": 0 ," +
                "    \"buckets\": [" +
                "      {\"key\": \"City 1\", \"doc_count\": 1}," +
                "      {\"key\": \"City 2\", \"doc_count\": 2}," +
                "      {\"key\": \"City 3\", \"doc_count\": 3}," +
                "      {\"key\": \"City 4\", \"doc_count\": 4}" +
                "    ]" +
                "  }" +
                "}";
        MockJestClient.configSearch(mockClient, EventsRepository.EVENTS_TYPE, 10, "[]", searchAggreg);

        List<CityLight> cityLightList = eventsEndPoint.allCities(null, "true");
        Assertions.assertThat(cityLightList).hasSize(4);
        Assertions.assertThat(cityLightList.get(0).count).isEqualTo(1);
        Assertions.assertThat(cityLightList.get(0).name).matches("City 1");
    }

    @Test
    public void should_find_a_city() {
        String searchHits = "[" +
                "  {" +
                "    \"_index\" : \"dev-conferences\"," +
                "    \"_type\" : \"events\"," +
                "    \"_id\" : \"1\"," +
                "    \"_source\" : {" +
                "        \"id\" : \"1\"," +
                "        \"city\" : \"the_city\"," +
                "        \"name\" : \"Event 1\"," +
                "        \"description\" : \"Event 1\"," +
                "        \"type\": \"CONFERENCE\"" +
                "    }" +
                "  }," +
                "  {" +
                "    \"_index\" : \"dev-conferences\"," +
                "    \"_type\" : \"events\"," +
                "    \"_id\" : \"2\"," +
                "    \"_source\" : {" +
                "        \"id\" : \"2\"," +
                "        \"city\" : \"the_city\"," +
                "        \"name\" : \"Event 2\"," +
                "        \"description\" : \"Event 2\"," +
                "        \"type\": \"CONFERENCE\"" +
                "    }" +
                "  }," +
                "  {" +
                "    \"_index\" : \"dev-conferences\"," +
                "    \"_type\" : \"events\"," +
                "    \"_id\" : \"3\"," +
                "    \"_source\" : {" +
                "        \"id\" : \"3\"," +
                "        \"city\" : \"the_city\"," +
                "        \"name\" : \"Commu 1\"," +
                "        \"description\" : \"Commu 1\"," +
                "        \"type\": \"COMMUNITY\"" +
                "    }" +
                "  }" +
                "]";
        MockJestClient.configSearch(mockClient, EventsRepository.EVENTS_TYPE, 3, searchHits, "{}");
        MockJestClient.configSearch(mockClient, EventsRepository.CALENDAREVENTS_TYPE, 0, "[]", "{}");

        City city = eventsEndPoint.city("the_city");
        Assertions.assertThat(city.id).matches("the_city");
        Assertions.assertThat(city.name).matches("the_city");
        Assertions.assertThat(city.communities).hasSize(1);
        Assertions.assertThat(city.communities.get(0).type).isEqualTo(Event.Type.COMMUNITY);
        Assertions.assertThat(city.conferences).hasSize(2);
        Assertions.assertThat(city.conferences.get(0).type).isEqualTo(Event.Type.CONFERENCE);
        Assertions.assertThat(city.conferences.get(1).type).isEqualTo(Event.Type.CONFERENCE);
    }

    @Test
    public void should_throw_event_type_exception() {
        String searchHits = "[" +
                "  {" +
                "    \"_index\" : \"dev-conferences\"," +
                "    \"_type\" : \"events\"," +
                "    \"_id\" : \"1\"," +
                "    \"_source\" : {" +
                "        \"id\" : \"1\"," +
                "        \"city\" : \"the_city\"," +
                "        \"name\" : \"Event 1\"," +
                "        \"description\" : \"Event 1\"," +
                "        \"type\": \"LOREM\"" + // THIS should throw an exception because event.type will be NULL...
                "    }" +
                "  }" +
                "]";
        MockJestClient.configSearch(mockClient, EventsRepository.EVENTS_TYPE, 1, searchHits, "{}");

        try {
            City city = eventsEndPoint.city("the_city");

            Assertions.failBecauseExceptionWasNotThrown(NullPointerException.class);
        } catch (NullPointerException e) {
            // GOOD !
        }
    }

    @Test
    public void should_detect_empty_ids_when_delete() {
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
            eventsRepository.deleteEvent("fsfv"); // Should pass
        } catch (RuntimeException e) {
            Assertions.fail("Should not throw an exception !", e);
        }
    }

    @Test
    public void should_find_calendar_events() {
        String searchHits = "[" +
                "  {" +
                "    \"_index\" : \"dev-conferences\"," +
                "    \"_type\" : \"calendarevents\"," +
                "    \"_id\" : \"1\"," +
                "    \"_source\" : {" +
                "        \"id\" : \"1\"," +
                "        \"name\" : \"Event 1\"," +
                "        \"description\" : \"Event 1\"," +
                "        \"url\" : \"http://www.example.com\"" +
                "    }" +
                "  }," +
                "  {" +
                "    \"_index\" : \"dev-conferences\"," +
                "    \"_type\" : \"calendarevents\"," +
                "    \"_id\" : \"2\"," +
                "    \"_source\" : {" +
                "        \"id\" : \"2\"," +
                "        \"name\" : \"Event 2\"," +
                "        \"description\" : \"Event 2\"," +
                "        \"url\" : \"http://www.example2.com\"" +
                "    }" +
                "  }" +
                "]";

        MockJestClient.configSearch(mockClient, EventsRepository.CALENDAREVENTS_TYPE, 2, searchHits, "{}");

        List<CalendarEvent> calendarEventList = eventsEndPoint.getCalendarEvents("10");
        Assertions.assertThat(calendarEventList).hasSize(2);
        Assertions.assertThat(calendarEventList.get(0).id).matches("1");
        Assertions.assertThat(calendarEventList.get(0).name).matches("Event 1");
        Assertions.assertThat(calendarEventList.get(0).description).matches("Event 1");
        Assertions.assertThat(calendarEventList.get(0).url).matches("http://www.example.com");
    }
}