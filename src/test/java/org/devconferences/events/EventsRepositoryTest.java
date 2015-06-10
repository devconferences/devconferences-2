package org.devconferences.events;

import org.assertj.core.api.Assertions;
import org.devconferences.elastic.ElasticUtils;
import org.elasticsearch.common.geo.GeoHashUtils;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EventsRepositoryTest extends ElasticsearchIntegrationTest {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return ImmutableSettings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(InternalNode.HTTP_ENABLED, true)
                .put("http.port", 9199 + nodeOrdinal) // starts at 9200
                .build();
    }

    @Before
    public void create_mapping() {
        ElasticUtils.createIndexIfNotExists();
    }

    @Test
    public void should_find_event() {
        Event event = new Event();
        event.id = UUID.randomUUID().toString();
        event.city = "the_city";
        event.description = "an awesome conf";

        EventsRepository eventsRepository = new EventsRepository();
        eventsRepository.createEvent(event);
        refresh();

        List<Event> matches = eventsRepository.search("awesome");
        Assertions.assertThat(matches).hasSize(1);
    }

    @Test
    public void should_find_event_around() {
        Event event = new Event();
        event.id = UUID.randomUUID().toString();
        event.location = new GeoPoint(1.0f, 1.0f);

        EventsRepository eventsRepository = new EventsRepository();
        eventsRepository.createEvent(event);
        refresh();

        eventsRepository.getEvent(event.id);

        Map<String, Long> events = eventsRepository.findEventsAround(1.0f, 1.0f, 10, 5);
        Assertions.assertThat(events).hasSize(1);
        String point = events.keySet().iterator().next();
        Assertions.assertThat(point).isEqualTo(GeoHashUtils.encode(1.0d, 1.0d, 5));
        Assertions.assertThat(events.values().iterator().next()).isEqualTo(1);
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
        refresh();

        eventsRepository.getEvent(event.id);

        Map<String, Long> events = eventsRepository.findEventsAround(50.0f, 50.0f, 10, 5);
        Assertions.assertThat(events).hasSize(1);
        String point = events.keySet().iterator().next();
        Assertions.assertThat(point).isEqualTo(GeoHashUtils.encode(50.0d, 50.0d, 5));
        Assertions.assertThat(events.values().iterator().next()).isEqualTo(1);
    }
}