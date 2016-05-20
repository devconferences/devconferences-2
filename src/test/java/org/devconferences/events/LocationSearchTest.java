package org.devconferences.events;

import org.assertj.core.api.Assertions;
import org.devconferences.elastic.DeveloppementESNode;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
import org.elasticsearch.common.geo.GeoHashUtils;
import org.elasticsearch.common.geo.GeoPoint;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.UUID;

public class LocationSearchTest {
    private static RuntimeJestClient jestClient;

    @BeforeClass
    public static void classSetUp() {
        DeveloppementESNode.createDevNode("9250");
        jestClient = ElasticUtils.createClient();
        // Index should not exists yet
        try { // This might throw randomly a SocketTimeoutException which we can't manage
            ElasticUtils.createIndex(); // Index + type creation
        } catch(Exception e) {
            if(e instanceof SocketTimeoutException) {
                System.out.println();
                System.out.println("SocketTimeoutException !");
                System.out.println();
            } else {
                System.out.println(e.getClass());
                throw e;
            }
        }
    }

    @AfterClass
    public static void tearDownOne() {
        ElasticUtils.deleteIndex();
    }

    @Test
    public void should_find_event_around() {
        Event event = new Event();
        event.id = UUID.randomUUID().toString();
        event.location = new GeoPoint(1.0f, 1.0f);

        EventsRepository eventsRepository = new EventsRepository();
        eventsRepository.createEvent(event);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Map<String, Long> events = eventsRepository.findEventsAround(1.0f, 1.0f, 10, 5);
        Assertions.assertThat(events).hasSize(1);
        String point = events.keySet().iterator().next();
        Assertions.assertThat(point).isEqualTo(GeoHashUtils.encode(1.0d, 1.0d, 5));
        Assertions.assertThat(events.values().iterator().next()).isEqualTo(1);

        eventsRepository.deleteEvent(event.id);
    }

    @Test
    public void should_not_find_event_far_away() {
        Event event = new Event();
        event.id = UUID.randomUUID().toString();
        event.location = new GeoPoint(1.0f, 1.0f);

        Event event2 = new Event();
        event2.id = UUID.randomUUID().toString();
        event2.location = new GeoPoint(50.0f, 50.0f);

        EventsRepository eventsRepository = new EventsRepository();
        eventsRepository.createEvent(event);
        eventsRepository.createEvent(event2);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Map<String, Long> events = eventsRepository.findEventsAround(50.0f, 50.0f, 10, 5);
        Assertions.assertThat(events).hasSize(1);
        String point = events.keySet().iterator().next();
        Assertions.assertThat(point).isEqualTo(GeoHashUtils.encode(50.0d, 50.0d, 5));
        Assertions.assertThat(events.values().iterator().next()).isEqualTo(1);

        eventsRepository.deleteEvent(event.id);
        eventsRepository.deleteEvent(event2.id);
    }
}
