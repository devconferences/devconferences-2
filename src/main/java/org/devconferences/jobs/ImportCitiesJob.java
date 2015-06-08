package org.devconferences.jobs;

import com.google.gson.Gson;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import org.devconferences.elastic.Elastic;
import org.devconferences.events.EventsRepository;
import org.devconferences.events.City;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

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

        Elastic.createIndexIfNotExists();

        URL v1Path = ImportCitiesJob.class.getResource("/v1");
        DirectoryStream<Path> directoryStream;
        try {
            directoryStream = Files.newDirectoryStream(FileSystems.getDefault().getPath(v1Path.getPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        directoryStream.forEach(path -> {
            try {
                City city = new Gson().fromJson(new FileReader(path.toString()), City.class);
                indexCity(city, eventsRepository);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void indexCity(City city, EventsRepository eventsRepository) {
        Index index = new Index.Builder(city).index(Elastic.DEV_CONFERENCES_INDEX).type(CITIES_TYPE).id(city.id).build();
        try {
            JestClient client = Elastic.createClient();
            client.execute(index);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
