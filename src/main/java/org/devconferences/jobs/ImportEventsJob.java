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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportEventsJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportEventsJob.class);
    // TODO, le temps de ...
    public static final String EVENTS_TYPE = "events";

    public static void main(String[] args) {
        createIndex();
    }

    public static void createIndex() {
        ElasticUtils.createIndexIfNotExists();

        importEvents();
    }

    public static void reloadEvents() {
        ElasticUtils.deleteData(EVENTS_TYPE);

        importEvents();

        EventsRepository er = new EventsRepository();
        CalendarEvent ce = new Gson().fromJson(new InputStreamReader(ImportEventsJob.class.getResourceAsStream("/calendar/testEvent.json")), CalendarEvent.class);
        er.indexOrUpdate(ce);
    }

    private static void importEvents() {
        EventsRepository eventsRepository = new EventsRepository();

        final int[] totalEvents = {0}; // For logging...
        LOGGER.info("Import events...");
        listEvents().forEach(path -> {
            Event event = new Gson().fromJson(new InputStreamReader(ImportEventsJob.class.getResourceAsStream(path)), Event.class);
            try {
                checkEvent(event, path); // This line might throw an exception

                // Default avatar
                if(event.avatar == null) {
                    event.avatar = "/img/no_logo.png";
                }
                event.city = path.split("/")[2]; // <null> / events / <city> / <idEvent>.json

                indexEvent(event, eventsRepository);
                totalEvents[0]++;
            } catch (RuntimeException e) {
                throw new RuntimeException(e.getMessage() + " - file path : " + path);
            }
        });
        LOGGER.info(totalEvents[0] + " events imported !");
    }

    public static void checkAllEvents() {
        listEvents().forEach(path -> {
            Event event = new Gson().fromJson(new InputStreamReader(ImportEventsJob.class.getResourceAsStream(path)), Event.class);
            try {
                checkEvent(event, path); // This line might throw an exception
            } catch (RuntimeException e) {
                throw new RuntimeException(e.getMessage() + " - file path : " + path);
            }
        });
    }

    public static void checkEvent(Event event, String path) {
        if(event.id == null) {
            throw new RuntimeException("Invalid Event : no 'id' field");
        }
        if(!(event.id + ".json").equals(path.split("/")[3])) {
            throw new RuntimeException("Invalid Event : filename and 'id' field mismatch");
        }
        if(event.type == null) {
            throw new RuntimeException("Invalid Event : no 'type' field");
        }
        if(event.name == null) {
            throw new RuntimeException("Invalid Event : no 'name' field");
        }
        if(event.description == null) {
            throw new RuntimeException("Invalid Event : no 'description' field");
        }
    }

    private static List<String> listEvents() {
        Path rootPath;
        HashMap<String,String> env = new HashMap<>();
        URL events = ImportEventsJob.class.getClassLoader().getResource("events");

        // Get root path of the jar
        if(events.getProtocol().equals("jar")) {
            try {
                FileSystem mountedJar = FileSystems.newFileSystem(events.toURI(), env);
                rootPath = mountedJar.getRootDirectories().iterator().next(); // There is only one...
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                File rootFile = new File(events.toURI());
                rootPath = rootFile.toPath();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            Stream<Path> res = Files.find(rootPath, 3,
                    (path, attr) -> isEventJSONFile(path, attr));
            return res.map(path -> path.toString())
                    .map(path -> {
                        if(!events.getProtocol().equals("jar")) {
                            // Replace absolute path with jar-like path
                            return path.replace(events.getPath(), "/events");
                        } else {
                            return path;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isEventJSONFile(Path path, BasicFileAttributes attr) {
        return path.toString().contains("/events/") && !attr.isDirectory() &&
                path.toString().endsWith(".json");
    }

    public static void indexEvent(Event event, EventsRepository eventsRepository) {
        eventsRepository.indexOrUpdate(event);
    }
}
