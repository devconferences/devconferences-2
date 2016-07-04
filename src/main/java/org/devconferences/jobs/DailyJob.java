package org.devconferences.jobs;

import io.searchbox.core.Delete;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.events.data.AbstractEvent;
import org.devconferences.events.data.CalendarEvent;
import org.devconferences.events.data.Event;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.devconferences.elastic.ElasticUtils.*;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

/**
 * Created by ronan on 21/06/16.
 */
public class DailyJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(DailyJob.class);
    private final ImportCalendarEventsJob importCalendarEventsJob;

    public DailyJob() {
        this(new ImportCalendarEventsJob());
    }

    public DailyJob(ImportCalendarEventsJob importCalendarEventsJob) {
        this.importCalendarEventsJob = importCalendarEventsJob;
    }

    public void doJob() {
        // 1. Ask @ Meetup upcoming events
        // 2. Remove CalendarEvent which date < current date();
        // 3. Remove Event and CalendarEvent which hidden == true
        RuntimeJestClient client = ElasticUtils.createClient();

        // Intro
        LOGGER.info("Start daily job...");

        // 1.
        ImportCalendarEventsJob.reloadMeetupIds();
        importCalendarEventsJob.askMeetupUpcomingEvents();

        // 2.
        removeOldCalendarEvents(client);

        // 3.
        removeHiddenEvents(client);
        removeHiddenCalendarEvents(client);

        // Outro
        LOGGER.info(new Date() + " : End of daily job.");
    }

    void removeOldCalendarEvents(RuntimeJestClient client) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(filteredQuery(null,
                FilterBuilders.rangeFilter("date").lte(System.currentTimeMillis())));
        searchSourceBuilder.query(queryBuilder.toString());

        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(DEV_CONFERENCES_INDEX).addType(CALENDAREVENTS_TYPE).build();

        SearchResult searchResult = client.execute(search);
        if(!searchResult.isSucceeded()) {
            throw new RuntimeException(searchResult.getErrorMessage());
        }

        List<String> idsList = searchResult.getHits(Event.class).stream().map(data -> data.source.id).collect(Collectors.toList());

        idsList.forEach(id -> {
            LOGGER.info("Deleting " + id + "...");
            Delete delete = new Delete.Builder(id)
                    .index(DEV_CONFERENCES_INDEX).type(CALENDAREVENTS_TYPE).build();

            DocumentResult documentResult = client.execute(delete);
            if(!documentResult.isSucceeded()) {
                LOGGER.error("Can't delete " + id + " : " + documentResult.getErrorMessage());
            }
        });
    }

    void removeHiddenEvents(RuntimeJestClient client) {
        genericDeleteHiddenData(client, Event.class, EVENTS_TYPE);
    }

    void removeHiddenCalendarEvents(RuntimeJestClient client) {
        genericDeleteHiddenData(client, CalendarEvent.class, CALENDAREVENTS_TYPE);
    }

    private void genericDeleteHiddenData(RuntimeJestClient client, Class<?> clazz, String type) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(termQuery("hidden", true));
        searchSourceBuilder.query(queryBuilder.toString());

        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(DEV_CONFERENCES_INDEX).addType(type).build();

        SearchResult searchResult = client.execute(search);
        if(!searchResult.isSucceeded()) {
            throw new RuntimeException(searchResult.getErrorMessage());
        }

        List<String> idsList = searchResult.getHits(clazz).stream()
                .map(data -> {
                    if(data.source instanceof AbstractEvent) {
                        return ((AbstractEvent) data.source).id;
                    } else {
                        throw new IllegalStateException("Unknown class : " + data.source.getClass());
                    }
                }).collect(Collectors.toList());

        idsList.forEach(id -> {
            LOGGER.info("Deleting " + id + "...");
            Delete delete = new Delete.Builder(id)
                    .index(DEV_CONFERENCES_INDEX).type(type).build();

            DocumentResult documentResult = client.execute(delete);
            if(!documentResult.isSucceeded()) {
                LOGGER.error("Can't delete " + id + " : " + documentResult.getErrorMessage());
            }
        });
    }
}
