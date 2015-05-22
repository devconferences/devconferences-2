package org.devconferences.v1;

import com.google.inject.Inject;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;

import java.net.URISyntaxException;
import java.util.List;

@Prefix("api/v1")
public class BackportV1Resource {

    private final AllConferences allConferences;

    @Inject
    public BackportV1Resource(AllConferences allConferences) {
        this.allConferences = allConferences;
    }

    @Get("/city/:id")
    public List<Conference> city(String id) throws URISyntaxException {
        return allConferences.forCity(id);
    }
}
