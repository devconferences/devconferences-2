package org.devconferences.events;

import com.google.inject.Inject;
import net.codestory.http.Context;
import net.codestory.http.annotations.*;
import net.codestory.http.errors.BadRequestException;
import net.codestory.http.errors.NotFoundException;
import org.devconferences.events.search.CalendarEventSearchResult;
import org.devconferences.events.search.CompletionResult;
import org.devconferences.events.search.EventSearchResult;
import org.devconferences.security.Authentication;
import org.devconferences.security.Encrypter;
import org.devconferences.users.User;
import org.devconferences.users.UsersRepository;

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
    private final Authentication authentication;

    @Inject
    public EventsEndPoint(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
        this.authentication = new Authentication(new Encrypter(), new UsersRepository());
    }

    @Gets({@Get("cities?q=:query"), @Get("cities/?q=:query")})
    @AllowOrigin("*")
    public List<CityLight> allCities(String query) {
        return eventsRepository.getAllCitiesWithQuery(query);
    }

    @Get("cities/:id?q=:query")
    @AllowOrigin("*")
    public City city(String id, String query) {
        return eventsRepository.getCity(id, query);
    }

    @Get("suggest?q=:query")
    @AllowOrigin("*")
    public CompletionResult suggest(String query, Context context) {
        return eventsRepository.suggest(query, authentication.getUser(context));
    }

    @Get("search/events?q=:query&page=:page&limit=:limit")
    @AllowOrigin("*")
    public EventSearchResult eventsSearch(String query, Integer page, Integer limit) {
        EventSearchResult result;
        try {
            result = eventsRepository.searchEvents(query, page, limit);
        } catch(RuntimeException e) {
            if(e.getMessage() != null && e.getMessage().startsWith("HTML 400 :")) {
                throw new BadRequestException();
            } else {
                throw e;
            }
        }
        return result;
    }

    @Get("search/calendar?q=:query&page=:page&limit=:limit")
    @AllowOrigin("*")
    public CalendarEventSearchResult eventsCalendarSearch(String query, Integer page, Integer limit) {
        CalendarEventSearchResult result;
        try {
            result = eventsRepository.searchCalendarEvents(query, page, limit);
        } catch(RuntimeException e) {
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

    @Get("calendar/:id")
    @AllowOrigin("*")
    public CalendarEvent getCalendarEvent(String id) {
        return NotFoundException.notFoundIfNull(eventsRepository.getCalendarEvent(id));
    }

    @Post("events/")
    @AllowOrigin("*")
    public void createEvent(Event event) {
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
    public List<CalendarEvent> getCalendarEvents(String page) {
        return NotFoundException.notFoundIfNull(eventsRepository.getCalendarEventList(page));
    }

    private void checkUsersEvent(String eventId, Context context) {
        User user = (User) context.currentUser();
        if(user.isInRole(EVENT_MANAGER)) {
            checkArgument(user.events.contains(eventId));
        }
    }


}
