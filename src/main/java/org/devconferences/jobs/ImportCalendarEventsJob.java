package org.devconferences.jobs;

import com.google.gson.*;
import org.devconferences.elastic.GeoPointAdapter;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.events.CalendarEvent;
import org.devconferences.meetup.MeetupApiClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

public class ImportCalendarEventsJob extends AbstractImportJSONJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCalendarEventsJob.class);
    public static final String CALENDAREVENTS_TYPE = "calendarevents";

    static final HashSet<String> idMeetupList = new HashSet<>();

    protected MeetupApiClient meetupApiClient;

    public ImportCalendarEventsJob() {
        super();
        meetupApiClient = new MeetupApiClient();
    }

    public ImportCalendarEventsJob(RuntimeJestClient client, MeetupApiClient meetupApiClient) {
        super(client);
        this.meetupApiClient = meetupApiClient;
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
            LOGGER.info("List of Meetup ids loaded ! Size : " + idMeetupList.size());
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
            totalCalendarEvents += askMeetupUpcomingEvents();
        }
        totalCalendarEvents += importJsonInFolder("calendar", CalendarEvent.class, this::removeHTMLTagsAndAddNewlines);

        saveMeetupIdList();

        return totalCalendarEvents;
    }

    private Object removeHTMLTagsAndAddNewlines(Object obj, String path) {
        if(obj instanceof CalendarEvent) {
            CalendarEvent calendarEvent = (CalendarEvent) obj;
            // Prefix JSON file id with 'file_' (distinction with Meetup imports)
            if(path != null) {
                calendarEvent.id = "file_" + calendarEvent.id;
            }
            // Replace <p></p> and <br/> with \n (ReactJS will detect it), and remove others HTML tags
            calendarEvent.description = calendarEvent.description.replaceAll("</p>", "\n")
                    .replaceAll("<br/>", "\n").replaceAll("<[^>]*>","")
                    .replaceAll("&amp;", "&") // Fix missed &
                    .replaceAll("(\\n\\s*)+", "\n"); // Only one newline

            if(calendarEvent.date < System.currentTimeMillis()) {
                return null;
            }
        }
        return obj;
    }

    @Override
    public void checkAllData() {
        checkAllDataInFolder("calendar");
    }

    @Override
    public void checkData(String path) {
        CalendarEvent event = new GsonBuilder().registerTypeAdapter(GeoPoint.class, new GeoPointAdapter()).create()
                .fromJson(new InputStreamReader(ImportEventsJob.class.getResourceAsStream(path)), CalendarEvent.class);
        try {
            checkCalendarEvent(event, path); // This line might throw an exception
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage() + " - file path : " + path);
        }
    }

    public static void checkCalendarEvent(CalendarEvent event, String path) {
        String[] pathSplit = path.split("/");
        String year = pathSplit[2];
        String month = pathSplit[3];
        String file = pathSplit[4];

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(event.date);

        // Check path
        try {
            Integer.decode(year);
        } catch(NumberFormatException e) {
            throw new RuntimeException("Invalid CalendarEvent : year in path is NaN");
        }
        try {
            Integer.decode(month);
        } catch(NumberFormatException e) {
            throw new RuntimeException("Invalid CalendarEvent : month in path is NaN");
        }

        int yearInt = Integer.parseInt(year);
        int monthInt = Integer.parseInt(month);

        // Check file content (mandatory fields)
        if(event.id == null) {
            throw new RuntimeException("Invalid CalendarEvent : no 'id' field");
        }
        if(event.name == null) {
            throw new RuntimeException("Invalid CalendarEvent : no 'name' field");
        }
        if(event.date == 0) {
            throw new RuntimeException("Invalid CalendarEvent : no 'date' field");
        }
        if(event.description == null) {
            throw new RuntimeException("Invalid CalendarEvent : no 'description' field");
        }

        // Check path and file content
        if(!(event.id + ".json").equals(file)) {
            throw new RuntimeException("Invalid CalendarEvent : filename and 'id' field mismatch");
        }
        if(calendar.get(Calendar.YEAR) != yearInt) {
            throw new RuntimeException("Invalid CalendarEvent : year path and 'date' field mismatch\n" +
                    "date year:" + calendar.get(Calendar.YEAR) + "\n" +
                    "path year:" + yearInt);
        }
        if((calendar.get(Calendar.MONTH) + 1) != monthInt) { // JANUARY = 0
            throw new RuntimeException("Invalid CalendarEvent : month path and 'date' field mismatch\n" +
                    "date month:" + (calendar.get(Calendar.MONTH) + 1) + "\n" +
                    "path month:" + monthInt);
        }
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

    private int askMeetupUpcomingEvents() {
        final int[] totalMeetupImport = {0, 0};

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

                totalMeetupImport[1]++;
                LOGGER.info("Total Meetup requests : " + totalMeetupImport[1] + "/" + idMeetupList.size());


            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        LOGGER.info(totalMeetupImport[0] + " events imported from Meetup !");

        return totalMeetupImport[0];
    }
}
