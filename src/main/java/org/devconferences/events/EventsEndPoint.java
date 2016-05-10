package org.devconferences.events;

import com.google.inject.Inject;
import net.codestory.http.Context;
import net.codestory.http.annotations.*;
import net.codestory.http.errors.NotFoundException;
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

    @Gets({@Get("cities"), @Get("cities/")})
    @AllowOrigin("*")
    public List<CityLight> allCities() {
        return eventsRepository.getAllCities();
    }

    @Get("cities/:id")
    @AllowOrigin("*")
    public City city(String id) {
        return eventsRepository.getCity(id);
    }

    @Get("events/search?q=:query")
    @AllowOrigin("*")
    public EventSearch eventsSearch(String query) {
        return eventsRepository.search(query);
    }

    @Get("events/:id")
    @AllowOrigin("*")
    public Event getEvent(String id) {
        return NotFoundException.notFoundIfNull(eventsRepository.getEvent(id));
    }

    @Post("events/")
    @AllowOrigin("*")
    public void createEvent(Event event){
        eventsRepository.createEvent(event);
    }

    @Put("events/:id")
    @AllowOrigin("*")
    @Roles({ADMIN, EVENT_MANAGER})
    public void updateEvent(String id, Event event, Context context) {
        checkUsersEvent(event.id, context);
        eventsRepository.indexOrUpdate(event);
    }

    @Delete("events/:id")
    @AllowOrigin("*")
    @Roles({ADMIN, EVENT_MANAGER})
    public void deleteEvent(String eventId, Context context) {
        checkUsersEvent(eventId, context);
        eventsRepository.deleteEvent(eventId);
    }

    private void checkUsersEvent(String eventId, Context context) {
        User user = (User) context.currentUser();
        if (user.isInRole(EVENT_MANAGER)) {
            checkArgument(user.events.contains(eventId));
        }
    }


}
