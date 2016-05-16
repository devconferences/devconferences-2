package org.devconferences.jobs;

import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.events.CalendarEvent;
import org.devconferences.meetup.MeetupApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.function.ObjDoubleConsumer;

public class ImportCalendarEventsJob extends AbstractImportJSONJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCalendarEventsJob.class);
    public static final String CALENDAREVENTS_TYPE = "calendarevents";

    private static final HashSet<String> idMeetupList = new HashSet<>();

    public static void main(String[] args) {
        ImportCalendarEventsJob importCalendarEventsJob = new ImportCalendarEventsJob();
        importCalendarEventsJob.createIndex();
    }

    public static boolean addIdMeetup(String idMeetup) {
        return idMeetupList.add(idMeetup);
    }

    public static boolean removeIdMeetup(String idMeetup) {
        return idMeetupList.remove(idMeetup);
    }

    @Override
    public void reloadData() {
        ElasticUtils.deleteData(CALENDAREVENTS_TYPE);

        askMeetupUpcomingEvents();
        importJsonInFolder("calendar", CalendarEvent.class, (obj, path) -> obj);
    }

    @Override
    public void checkAllData() {
        // Check nothing... Yet.
    }

    @Override
    public void checkData(String path) {
        // Check nothing... Yet.
    }

    private void askMeetupUpcomingEvents() {
        final int[] totalMeetupImport = {0};
        try(RuntimeJestClient client = ElasticUtils.createClient();) {
            MeetupApiClient meetupApiClient = new MeetupApiClient();

            idMeetupList.forEach(id -> {
                try {
                    List<CalendarEvent> listCalendarEvent = meetupApiClient.getUpcomingEvents(id);

                    listCalendarEvent.forEach(data -> {
                        if(data.description == null) {
                            data.description = "Pas de description.";
                        }
                        client.indexES(CALENDAREVENTS_TYPE, data, data.id);
                        totalMeetupImport[0]++;
                    });

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        LOGGER.info(totalMeetupImport[0] + " events imported with Meetup !");
    }
}
