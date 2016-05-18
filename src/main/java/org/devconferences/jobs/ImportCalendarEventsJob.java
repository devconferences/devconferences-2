package org.devconferences.jobs;

import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.events.CalendarEvent;
import org.devconferences.meetup.MeetupApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashSet;
import java.util.List;

public class ImportCalendarEventsJob extends AbstractImportJSONJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCalendarEventsJob.class);
    public static final String CALENDAREVENTS_TYPE = "calendarevents";

    static final HashSet<String> idMeetupList = new HashSet<>();

    public ImportCalendarEventsJob() {
        super();
    }

    public ImportCalendarEventsJob(RuntimeJestClient client) {
        super(client);
    }

    public static void main(String[] args) {
        ElasticUtils.createIndex();
        ImportCalendarEventsJob importCalendarEventsJob = new ImportCalendarEventsJob();
        importCalendarEventsJob.reloadData(false);
    }

    public static boolean addIdMeetup(String idMeetup) {
        return idMeetupList.add(idMeetup);
    }

    public static boolean removeIdMeetup(String idMeetup) {
        return idMeetupList.remove(idMeetup);
    }

    public static void reloadMeetupIds() {
        // Deserialization of Meetup Ids
        ObjectInputStream objectInputStream = null;

        try {
            final FileInputStream file = new FileInputStream(".meetupIdList");
            objectInputStream = new ObjectInputStream(file);
            HashSet<String> backupMeetupIds = null;
            backupMeetupIds = (HashSet<String>) objectInputStream.readObject();

            backupMeetupIds.forEach(ImportCalendarEventsJob::addIdMeetup);
            LOGGER.info("List of Meetup ids loaded !");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public int reloadData(boolean noRemoteCall) {
        client.deleteAllES(CALENDAREVENTS_TYPE);

        int totalCalendarEvents = 0;

        if(!noRemoteCall) {
            askMeetupUpcomingEvents();
        }
        totalCalendarEvents += importJsonInFolder("calendar", CalendarEvent.class, this::removeHTMLTagsAndAddNewlines);

        saveMeetupIdList();

        return totalCalendarEvents;
    }

    @Override
    public void checkAllData() {
        // Check nothing... Yet.
    }

    private Object removeHTMLTagsAndAddNewlines(Object obj, String path) {
        if(obj instanceof CalendarEvent) {
            CalendarEvent calendarEvent = (CalendarEvent) obj;
            // Replace <p></p> and <br/> with \n (ReactJS will detect it), and remove others HTML tags
            calendarEvent.description = calendarEvent.description.replaceAll("</p>", "\n")
                    .replaceAll("<br/>", "\n").replaceAll("<[^>]*>","")
                    .replaceAll("&amp;", "&") // Fix missed &
                    .replaceAll("(\\n\\s*)+", "\n"); // Only one newline
        }

        return obj;
    }

    @Override
    public void checkData(String path) {
        // Check nothing... Yet.
    }

    private static void saveMeetupIdList() {
        // Serialization of Meetup ids for cron update
        LOGGER.info("Saving Meetup ids...");
        ObjectOutputStream objectOutputStream = null;
        try {
            final FileOutputStream file = new FileOutputStream(".meetupIdList");
            objectOutputStream = new ObjectOutputStream(file);
            objectOutputStream.writeObject(idMeetupList);
            LOGGER.info("List of Meetup ids saved !");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(objectOutputStream != null) {
                try {
                    objectOutputStream.flush();
                    objectOutputStream.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void askMeetupUpcomingEvents() {
        final int[] totalMeetupImport = {0};
        MeetupApiClient meetupApiClient = new MeetupApiClient();

        LOGGER.info("Import events from Meetup...");

        idMeetupList.forEach(id -> {
            try {
                List<CalendarEvent> listCalendarEvent = meetupApiClient.getUpcomingEvents(id);

                listCalendarEvent.forEach(data -> {
                    if(data.description == null) {
                        data.description = "Pas de description.";
                    }
                    removeHTMLTagsAndAddNewlines(data, null);
                    client.indexES(CALENDAREVENTS_TYPE, data, data.id);
                    totalMeetupImport[0]++;
                });

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        LOGGER.info(totalMeetupImport[0] + " events imported from Meetup !");
    }
}
