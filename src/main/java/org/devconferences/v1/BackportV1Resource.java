package org.devconferences.v1;

import com.google.inject.Inject;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;

import java.util.List;

@Prefix("api/v1")
public class BackportV1Resource {

    private final BackportV1Data backportV1Data;

    @Inject
    public BackportV1Resource(BackportV1Data backportV1Data) {
        this.backportV1Data = backportV1Data;
    }

    @Get("/city")
    public List<CityLight> cities() {
        return backportV1Data.cities();
    }

    @Get("/city/:id")
    public City cityConferencies(String id) {
        return backportV1Data.city(id);
    }
}
