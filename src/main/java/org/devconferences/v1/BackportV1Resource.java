package org.devconferences.v1;

import com.google.inject.Inject;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import org.devconferences.events.EventsRepository;
import org.devconferences.events.City;
import org.devconferences.events.CityLight;

import java.util.List;

@Prefix("api/v1")
public class BackportV1Resource {

    private final BackportV1Data backportV1Data;
    private final EventsRepository eventsRepository;

    @Inject
    public BackportV1Resource(BackportV1Data backportV1Data, EventsRepository eventsRepository) {
        this.backportV1Data = backportV1Data;
        this.eventsRepository = eventsRepository;
    }

    @Get("/city")
    public List<CityLight> cities() {
        return eventsRepository.getAllCities();
    }

    @Get("/city/:id")
    public City cityConferencies(String id) {
        return eventsRepository.getCity(id);
    }

}
