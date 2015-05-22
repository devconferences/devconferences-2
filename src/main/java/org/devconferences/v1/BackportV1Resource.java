package org.devconferences.v1;

import com.google.inject.Inject;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;

import java.util.List;
import java.util.Set;

@Prefix("api/v1")
public class BackportV1Resource {

    private final BackportV1Data backportV1Data;

    @Inject
    public BackportV1Resource(BackportV1Data backportV1Data) {
        this.backportV1Data = backportV1Data;
    }

    @Get("/city")
    public Set<String> cities() {
        return backportV1Data.cities();
    }

    @Get("/city/:cityId")
    public List<Conference> cityConferencies(String cityId) {
        return backportV1Data.conferencesForCity(cityId);
    }
}
