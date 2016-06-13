package org.devconferences.jobs;


import io.searchbox.core.Delete;
import org.assertj.core.api.Assertions;
import org.devconferences.elastic.MockJestClient;
import org.devconferences.elastic.RuntimeJestClientAdapter;
import org.devconferences.events.Event;
import org.devconferences.events.EventsRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ImportEventsJobTest {
    private ImportEventsJob importEventsJob;
    private RuntimeJestClientAdapter mockClient;

    @Before
    public void setUp() {
        mockClient = MockJestClient.createMock();
        importEventsJob = new ImportEventsJob(mockClient);
    }

    @Test
    public void testEventCheck() {
        Event event = new Event();
        try {
            ImportEventsJob.checkEvent(event, "/events/the_city/test.json");

            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch (RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid Event : no 'id' field");
        }
        try {
            event.id = "test1";
            ImportEventsJob.checkEvent(event, "/events/the_city/test.json");

            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch (RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid Event : filename and 'id' field mismatch");
        }
        try {
            event.id = "test";
            ImportEventsJob.checkEvent(event, "/events/the_city/test.json");

            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch (RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid Event : no 'type' field");
        }
        try {
            event.type = Event.Type.COMMUNITY;
            ImportEventsJob.checkEvent(event, "/events/the_city/test.json");

            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch (RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid Event : no 'name' field");
        }
        try {
            event.name = "Event test";
            ImportEventsJob.checkEvent(event, "/events/the_city/test.json");

            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch (RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid Event : no 'description' field");
        }
        try {
            event.description = "Pas de description";
            ImportEventsJob.checkEvent(event, "/events/the_city/test.json"); // Should be OK here
        } catch (RuntimeException e) {
            Assertions.fail("Should not throw an exception !", e);
        }
    }

    @Test
    public void testReloadData() {
        int totalImportedFiles = importEventsJob.reloadData(true);
        when(mockClient.execute(isA(Delete.class))).thenReturn(null);
        Assertions.assertThat(totalImportedFiles).isEqualTo(2);
    }

    @Test
    public void testCheckAllData() {
        try {
            importEventsJob.checkAllData();
        } catch(RuntimeException e) {
            Assertions.fail("Should not throw an exception !", e);
        }
    }

    @Test
    public void testCheckDataFail() {
        try {
            importEventsJob.checkData("/events_fail/the_city_fail/testeventfail1.json");
        } catch(RuntimeException e) {
            Assertions.assertThat(e.getMessage()).matches("Invalid Event : filename and 'id' field mismatch - file path : /events_fail/the_city_fail/testeventfail1.json");
        }
    }
}