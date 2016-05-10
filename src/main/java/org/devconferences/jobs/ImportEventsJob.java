package org.devconferences.jobs;

import com.google.gson.Gson;
import io.searchbox.core.Index;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.devconferences.events.Event.Type.COMMUNITY;
import static org.devconferences.events.Event.Type.CONFERENCE;


public class ImportEventsJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportEventsJob.class);
    // TODO, le temps de ...
    public static final String EVENTS_TYPE = "events";

    public static void main(String[] args) {
        runJob();
    }

    public static void runJob() {
        EventsRepository eventsRepository = new EventsRepository();
        final int[] totalEvents = {0}; // For logging...

        ElasticUtils.createIndexIfNotExists();

        LOGGER.info("Import events...");
        listEvents().forEach(path -> {
            Event event = new Gson().fromJson(new InputStreamReader(ImportEventsJob.class.getResourceAsStream(path)), Event.class);
            try {
                checkEvent(event, path); // This line might throw an exception
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
            throw new RuntimeException("Invalid Event : missed 'id' field");
        }
        if(!(event.id + ".json").equals(path.split("/")[3])) {
            throw new RuntimeException("Invalid Event : filename and 'id' field mismatch");
        }
        if(event.type == null) {
            throw new RuntimeException("Invalid Event : missed 'type' field");
        }
        if(event.name == null) {
            throw new RuntimeException("Invalid Event : missed 'name' field");
        }
        if(event.description == null) {
            throw new RuntimeException("Invalid Event : missed 'description' field");
        }
    }

    private static List<String> listEvents() {
        Path rootPath = null;
        HashMap<String,String> env = new HashMap<>();
        URL events = ImportEventsJob.class.getClassLoader().getResource("events");

        // Get root path of the jar
        if(events.getProtocol().equals("jar")) {
            try {
                FileSystem mountedJar = FileSystems.newFileSystem(events.toURI(), env);
                rootPath = mountedJar.getRootDirectories().iterator().next(); // There is only one...
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                File rootFile = new File(events.toURI());
                rootPath = rootFile.toPath();
            } catch (URISyntaxException e) {
                e.printStackTrace();
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
            e.printStackTrace();
        }

        return new ArrayList<>(); // Avoid NullPointerException
    }

    private static boolean isEventJSONFile(Path path, BasicFileAttributes attr) {
        return path.toString().contains("/events/") && !attr.isDirectory() &&
                path.toString().endsWith(".json");
    }

    public static void indexEvent(Event event, EventsRepository eventsRepository) {
        eventsRepository.indexOrUpdate(event);
    }
}
