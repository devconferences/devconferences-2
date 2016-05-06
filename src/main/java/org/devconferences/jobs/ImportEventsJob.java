package org.devconferences.jobs;

import com.google.gson.Gson;
import io.searchbox.core.Index;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.events.City;
import org.devconferences.events.Event;
import org.devconferences.events.EventsRepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.devconferences.events.Event.Type.COMMUNITY;
import static org.devconferences.events.Event.Type.CONFERENCE;


public class ImportEventsJob {

    // TODO, le temps de ...
    public static final String EVENTS_TYPE = "events";

    public static void main(String[] args) {
        runJob();
    }

    public static void runJob() {
        EventsRepository eventsRepository = new EventsRepository();

        ElasticUtils.createIndexIfNotExists();

        listEvents().forEach(path -> {
            Event event = new Gson().fromJson(new InputStreamReader(ImportEventsJob.class.getResourceAsStream(path.toString())), Event.class);
            event.city = path.split("/")[2]; // <null> / events / <city> / <idEvent>.json
            indexEvent(event, eventsRepository);
        });
    }

    private static List<String> listEvents() {
        final File jarFile = new File(ImportEventsJob.class.getProtectionDomain().getCodeSource().getLocation().getPath());

        if (jarFile.isFile()) {
            try (final JarFile jar = new JarFile(jarFile);) {
                return Collections.list(jar.entries()).stream()
                        .filter(jarEntry -> {
                            return jarEntry.toString().startsWith("events") && !jarEntry.isDirectory();
                        })
                        .map(jarEntry -> {
                            return "/" + jarEntry.toString();
                        })
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            DirectoryStream<Path> directoryStream;
            try {
                String v1Path = ImportEventsJob.class.getResource("/events").getPath();
                directoryStream = Files.newDirectoryStream(FileSystems.getDefault().getPath(v1Path));
                return StreamSupport.stream(directoryStream.spliterator(), false)
                        .map(path -> {
                            return "/events" + path.toString().substring(v1Path.length());
                        })
                        .collect(Collectors.toList());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void indexEvent(Event event, EventsRepository eventsRepository) {
        Index index = new Index.Builder(event).index(ElasticUtils.DEV_CONFERENCES_INDEX).type(EVENTS_TYPE).id(event.id).build();
        try (RuntimeJestClient client = ElasticUtils.createClient();) {
            client.execute(index);
        }
    }
}
