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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static net.codestory.http.errors.NotFoundException.notFoundIfNull;

@Singleton
public class BackportV1Data {

    private final Map<String, List<Conference>> allConferences = new TreeMap<>();

    public BackportV1Data() throws Exception {
        Path dir = Paths.get(this.getClass().getResource("/v1").toURI());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                Type conferenceListType = new TypeToken<List<Conference>>() {
                }.getType();
                allConferences.put(
                        entry.getFileName().toString().replace(".json", ""),
                        new Gson().fromJson(new FileReader(entry.toFile()), conferenceListType)
                );
            }
        }
    }

    public List<Conference> conferencesForCity(String city) {
        return notFoundIfNull(allConferences.get(city));
    }

    public Set<String> cities() {
        return allConferences.keySet();
    }
}
