package org.devconferences.jobs;

import org.assertj.core.api.Assertions;
import org.devconferences.elastic.DeveloppementESNode;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.events.Event;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ImportEventsJobTest {
    private ImportEventsJob importEventsJob;

    @BeforeClass
    public static void classSetUp() {
        DeveloppementESNode.createDevNode("9250");
        ElasticUtils.createIndex();
    }

    @AfterClass
    public static void tearDownOne() {
        ElasticUtils.deleteIndex();
    }

    @Before
    public void setUp() {
        importEventsJob = new ImportEventsJob();
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