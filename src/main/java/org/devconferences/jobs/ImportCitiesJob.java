package org.devconferences.jobs;

import com.google.gson.Gson;
import io.searchbox.core.Index;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.events.City;
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


public class ImportCitiesJob {

    // TODO, le temps de ...
    public static final String CITIES_TYPE = "cities";

    public static void main(String[] args) {
        runJob();
    }

    public static void runJob() {
        EventsRepository eventsRepository = new EventsRepository();

        ElasticUtils.createIndexIfNotExists();

        listV1Entries().forEach(path -> {
            City city = new Gson().fromJson(new InputStreamReader(ImportCitiesJob.class.getResourceAsStream(path.toString())), City.class);
            indexCity(city, eventsRepository);
        });
    }

    private static List<String> listV1Entries() {
        final File jarFile = new File(ImportCitiesJob.class.getProtectionDomain().getCodeSource().getLocation().getPath());

        if (jarFile.isFile()) {
            try (final JarFile jar = new JarFile(jarFile);) {
                return Collections.list(jar.entries()).stream()
                        .filter(jarEntry -> {
                            return jarEntry.toString().startsWith("v1") && !jarEntry.isDirectory();
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
                String v1Path = ImportCitiesJob.class.getResource("/v1").getPath();
                directoryStream = Files.newDirectoryStream(FileSystems.getDefault().getPath(v1Path));
                return StreamSupport.stream(directoryStream.spliterator(), false)
                        .map(path -> {
                            return "/v1" + path.toString().substring(v1Path.length());
                        })
                        .collect(Collectors.toList());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void indexCity(City city, EventsRepository eventsRepository) {
        Index index = new Index.Builder(city).index(ElasticUtils.DEV_CONFERENCES_INDEX).type(CITIES_TYPE).id(city.id).build();
        try (RuntimeJestClient client = ElasticUtils.createClient();) {
            client.execute(index);
        }

        city.communities.stream().forEach(event -> {
            event.type = COMMUNITY;
            event.city = city.name;
            eventsRepository.indexOrUpdate(event);
        });
        city.conferences.stream().forEach(event -> {
            event.type = CONFERENCE;
            event.city = city.name;
            eventsRepository.indexOrUpdate(event);
        });
    }
}
