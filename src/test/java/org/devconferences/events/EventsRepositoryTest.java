package org.devconferences.events;

import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Test;

public class EventsRepositoryTest extends ElasticsearchIntegrationTest {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return ImmutableSettings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(InternalNode.HTTP_ENABLED, true)
                .put("http.port", 9199 + nodeOrdinal) // starts at 9200
                .build();
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
}