package org.devconferences.v1;

import com.google.gson.Gson;
import com.google.inject.Singleton;

import java.io.FileReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static net.codestory.http.errors.NotFoundException.notFoundIfNull;

@Singleton
public class BackportV1Data {

    private final Map<String, City> allCities = new TreeMap<>();

    public BackportV1Data() throws Exception {
        Path dir = Paths.get(this.getClass().getResource("/v1").toURI());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                City city = new Gson().fromJson(new FileReader(entry.toFile()), City.class);
                allCities.put(city.id, city);
            }
        }
    }

    public City city(String cityId) {
        return notFoundIfNull(allCities.get(cityId));
    }

    public List<CityLight> cities() {
        return allCities.entrySet()
                .stream()
                .map(entry -> {
                    City city = entry.getValue();
                    return new CityLight(city.id, city.name);
                })
                .collect(Collectors.toList());
    }
}
