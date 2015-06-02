package org.devconferences.v1;

import com.google.inject.Inject;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import org.devconferences.elastic.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Prefix("api/v1")
public class BackportV1Resource {

    private final BackportV1Data backportV1Data;
    private final Repository repository;

    @Inject
    public BackportV1Resource(BackportV1Data backportV1Data, Repository repository) {
        this.backportV1Data = backportV1Data;
        this.repository = repository;
    }

    @Get("/city")
    public List<CityLight> cities() {
        return repository.getAllCities().stream().map(city -> {
            return new CityLight(city.id, city.name);
        }).collect(Collectors.toList());
    }

    @Get("/city/:id")
    public City cityConferencies(String id) {
        return repository.getCity(id);

    }
}
