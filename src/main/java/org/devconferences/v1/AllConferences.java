package org.devconferences.v1;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Singleton;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.codestory.http.errors.NotFoundException.notFoundIfNull;

@Singleton
public class AllConferences {

    private final Map<String, List<Conference>> allConferences = new HashMap<>();

    public AllConferences() throws Exception {
        Path dir = Paths.get(this.getClass().getResource("/v1").toURI());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                Type conferenceListType = new TypeToken<List<Conference>>() {
                }.getType();
                allConferences.put(
                        entry.getFileName().toString(),
                        new Gson().fromJson(new FileReader(entry.toFile()), conferenceListType)
                );
            }
        }


    }

    public List<Conference> forCity(String city) {
        return notFoundIfNull(allConferences.get(city + ".json"));
    }

}
