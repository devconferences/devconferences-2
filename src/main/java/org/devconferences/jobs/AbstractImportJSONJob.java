package org.devconferences.jobs;

import com.google.gson.GsonBuilder;
import io.searchbox.indices.Refresh;
import org.devconferences.elastic.ElasticUtils;
import org.devconferences.elastic.GeoPointAdapter;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.events.EventsRepository;
import org.elasticsearch.common.geo.GeoPoint;
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
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.devconferences.elastic.ElasticUtils.DEV_CONFERENCES_INDEX;

public abstract class AbstractImportJSONJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractImportJSONJob.class);
    protected RuntimeJestClient client;

    AbstractImportJSONJob() {
        client = ElasticUtils.createClient();
    }

    AbstractImportJSONJob(RuntimeJestClient client) {
        this.client = client;
    }

    public abstract int reloadData(boolean noRemoteCall);
    public abstract void checkData(String path);
    public abstract void checkAllData();

    void checkAllDataInFolder(String resourceFolderPath) {
        listFilesinFolder(resourceFolderPath).forEach(this::checkData);
    }

    int importJsonInFolder(String resourceFolderPath, Class<?> classInfo, BiFunction<Object, String, Object> forEachFunc) {
        EventsRepository eventsRepository = new EventsRepository(client);

        // Refresh ES on every document update (assume update are only when start server...)
        // Because users are return with a search, when update a lot of document, some notifications can be lost without this...
        final Refresh refresh = new Refresh.Builder().addIndex(DEV_CONFERENCES_INDEX).build();

        final int[] totalEvents = {0}; // For logging...
        LOGGER.info("Import data (" + classInfo.getSimpleName() + ")...");
        listFilesinFolder(resourceFolderPath).forEach(path -> {
            Object object = new GsonBuilder().registerTypeAdapter(GeoPoint.class, new GeoPointAdapter()).create()
                    .fromJson(new InputStreamReader(AbstractImportJSONJob.class.getResourceAsStream(path)), classInfo);
            Object object2 = forEachFunc.apply(object, path);
            if(object2 != null) {
                boolean isUpdated = eventsRepository.indexOrUpdate(classInfo.cast(object));
                if(isUpdated) {
                    client.execute(refresh);
                    totalEvents[0]++;
                }
            }
        });
        LOGGER.info(totalEvents[0] + " document(s) updated !");

        return totalEvents[0];
    }

    private List<String> listFilesinFolder(String resourceFolderPath) {
        Path rootPath;
        HashMap<String, String> env = new HashMap<>();
        URL calendarEvent = AbstractImportJSONJob.class.getClassLoader().getResource(resourceFolderPath);

        // Get root path of the jar
        if(calendarEvent.getProtocol().equals("jar")) {
            try(FileSystem mountedJar = FileSystems.newFileSystem(calendarEvent.toURI(), env)) {
                rootPath = mountedJar.getRootDirectories().iterator().next(); // There is only one...
            } catch(URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                File rootFile = new File(calendarEvent.toURI());
                rootPath = rootFile.toPath();
            } catch(URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            Stream<Path> res = Files.find(rootPath, 5,
                    (path, attr) -> isJSONFileInFolder(path, attr, resourceFolderPath));
            return res.map(Path::toString)
                    .map(path -> {
                        if(!calendarEvent.getProtocol().equals("jar")) {
                            // Replace absolute path with jar-like path
                            return path.replace(calendarEvent.getPath(), "/" + resourceFolderPath);
                        } else {
                            return path;
                        }
                    })
                    .collect(Collectors.toList());
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isJSONFileInFolder(Path path, BasicFileAttributes attr, String resourceFolderPath) {
        return path.toString().contains("/" + resourceFolderPath + "/") && !attr.isDirectory() &&
                path.toString().endsWith(".json");
    }
}
