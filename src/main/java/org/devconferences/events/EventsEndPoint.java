package org.devconferences.events;

import com.google.inject.Inject;
import net.codestory.http.Context;
import net.codestory.http.annotations.*;
import org.devconferences.users.User;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.devconferences.users.User.ADMIN;
import static org.devconferences.users.User.EVENT_MANAGER;

/**
 * Created by chris on 05/06/15.
 */
@Prefix("api/v2/")
public class EventsEndPoint {
    private final EventsRepository eventsRepository;

    @Inject
    public EventsEndPoint(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
    }

    @Gets({@Get("cities"), @Get("cities/")}) @AllowOrigin("*")
    public List<CityLight> allCities() {
        return eventsRepository.getAllCities();
    }

    @Get("cities/:id") @AllowOrigin("*")
    public City city(String id) {
        return eventsRepository.getCity(id);
    }

    @Get("events/search?q=:query") @AllowOrigin("*")
    public List<Event> eventsSearch(String query) {
        return eventsRepository.search(query);
    }

    @Put("events/:id") @AllowOrigin("*")
    @Roles({ADMIN, EVENT_MANAGER})
    public void updateEvent(String id, Event event, Context context) {
        checkUsersEvent(event, context);

        eventsRepository.indexEvent(event);
    }

    @Delete @AllowOrigin("*")
    @Roles({ADMIN, EVENT_MANAGER})
    public void deleteEvent(Event event, Context context) {
        checkUsersEvent(event, context);

        eventsRepository.deleteEvent(event);
    }

    private void checkUsersEvent(Event event, Context context) {
        User user = (User) context.currentUser();
        if (user.isInRole(EVENT_MANAGER)) {
            checkArgument(user.events.contains(event.id));
        }
    }


}
