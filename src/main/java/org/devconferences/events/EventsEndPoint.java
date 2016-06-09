package org.devconferences.events;

import com.google.inject.Inject;
import net.codestory.http.Context;
import net.codestory.http.annotations.*;
import net.codestory.http.errors.BadRequestException;
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

    @Gets({@Get("cities?q=:query&all=:all"), @Get("cities/?q=:query&all=:all")})
    @AllowOrigin("*")
    public List<CityLight> allCities(String query, String all) {
        return eventsRepository.getAllCitiesWithQuery(query, Boolean.parseBoolean(all));
    }

    @Get("cities/:id?q=:query")
    @AllowOrigin("*")
    public City city(String id, String query) {
        return eventsRepository.getCity(id, query);
    }

    @Get("suggest?q=:query")
    @AllowOrigin("*")
    public CompletionResult suggest(String query) {
        return eventsRepository.suggest(query);
    }

    @Get("search/events?q=:query&p=:page&lat=:lat&lon=:lon&dist=:distance&all=:all")
    @AllowOrigin("*")
    public AbstractSearchResult eventsSearch(String query, String page, String lat, String lon, String distance, String all) {
        AbstractSearchResult result;
        try {
            result = eventsRepository.searchEvents(query, page, lat, lon, distance, Boolean.parseBoolean(all));
        } catch (RuntimeException e) {
            if(e.getMessage() != null && e.getMessage().startsWith("HTML 400 :")) {
                throw new BadRequestException();
            } else {
                throw e;
            }
        }
        return result;
    }

    @Get("search/calendar?q=:query&p=:page&lat=:lat&lon=:lon&dist=:distance&all=:all")
    @AllowOrigin("*")
    public AbstractSearchResult eventsCalendarSearch(String query, String page, String lat, String lon, String distance, String all) {
        AbstractSearchResult result;
        try {
            result = eventsRepository.searchCalendarEvents(query, page, lat, lon, distance, Boolean.parseBoolean(all));
        } catch (RuntimeException e) {
            if(e.getMessage() != null && e.getMessage().startsWith("HTML 400 :")) {
                throw new BadRequestException();
            } else {
                throw e;
            }
        }
        return result;
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

    @Get("calendar?p=:page")
    @AllowOrigin("*")
    public List<CalendarEvent> getCalendarEvents (String page) {
        return NotFoundException.notFoundIfNull(eventsRepository.getCalendarEvents(page));
    }

    private void checkUsersEvent(String eventId, Context context) {
        User user = (User) context.currentUser();
        if (user.isInRole(EVENT_MANAGER)) {
            checkArgument(user.events.contains(eventId));
        }
    }


}
