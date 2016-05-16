package org.devconferences.jobs;

import com.google.gson.Gson;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.events.CalendarEvent;
import org.devconferences.events.Event;
import org.devconferences.events.EventsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportCalendarEventsJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCalendarEventsJob.class);
    // TODO, le temps de ...
    public static final String CALENDAREVENTS_TYPE = "calendarevents";

    public static void main(String[] args) {
        createIndex();
    }

    public static void createIndex() {
        ElasticUtils.createIndexIfNotExists();

        askMeetupUpcomingEvents();
        importCalendarEvents();
    }

    public static void reloadCalendarEvents() {
        ElasticUtils.deleteData(CALENDAREVENTS_TYPE);

        askMeetupUpcomingEvents();
        importCalendarEvents();
    }


    private static void askMeetupUpcomingEvents() {
    }

    private static void importCalendarEvents() {
        EventsRepository eventsRepository = new EventsRepository();

        final int[] totalEvents = {0}; // For logging...
        LOGGER.info("Import calendar events...");
        listCalendarEvents().forEach(path -> {
            CalendarEvent calendarEvent = new Gson().fromJson(new InputStreamReader(ImportCalendarEventsJob.class.getResourceAsStream(path)), CalendarEvent.class);
            eventsRepository.indexOrUpdate(calendarEvent);
            totalEvents[0]++;
        });
        LOGGER.info(totalEvents[0] + " calendar events imported !");
    }

    private static List<String> listCalendarEvents() {
        Path rootPath;
        HashMap<String,String> env = new HashMap<>();
        URL calendarEvent = ImportCalendarEventsJob.class.getClassLoader().getResource("calendar");

        // Get root path of the jar
        if(calendarEvent.getProtocol().equals("jar")) {
            try {
                FileSystem mountedJar = FileSystems.newFileSystem(calendarEvent.toURI(), env);
                rootPath = mountedJar.getRootDirectories().iterator().next(); // There is only one...
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                File rootFile = new File(calendarEvent.toURI());
                rootPath = rootFile.toPath();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            Stream<Path> res = Files.find(rootPath, 3,
                    (path, attr) -> isCalendarEventJSONFile(path, attr));
            return res.map(path -> path.toString())
                    .map(path -> {
                        if(!calendarEvent.getProtocol().equals("jar")) {
                            // Replace absolute path with jar-like path
                            return path.replace(calendarEvent.getPath(), "/calendar");
                        } else {
                            return path;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isCalendarEventJSONFile(Path path, BasicFileAttributes attr) {
        return path.toString().contains("/calendar/") && !attr.isDirectory() &&
                path.toString().endsWith(".json");
    }
}
