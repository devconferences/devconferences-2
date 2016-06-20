package org.devconferences.users;

import org.assertj.core.api.Assertions;
import org.devconferences.elastic.*;
import org.devconferences.events.CalendarEvent;
import org.devconferences.events.EventsRepository;
import org.devconferences.events.search.SimpleSearchResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class UserRepositoryTest {
    private static UsersRepository usersRepository;
    private static EventsRepository eventsRepository;
    private static User user;

    @BeforeClass
    public static void classSetUp() {
        DeveloppementESNode.createDevNode("9250");
        ElasticUtils.createIndex();

        eventsRepository = new EventsRepository();
        usersRepository = new UsersRepository(ElasticUtils.createClient(), eventsRepository);
        user = new User("abc1234", "1243012.0", "foo@devconferences.org", "/img/no_logo.png");

        CalendarEvent calendarEvent1 = new CalendarEvent();
        calendarEvent1.id = "1";
        calendarEvent1.name = "Event 1";
        calendarEvent1.description = "Event 1 - In Favourite";
        calendarEvent1.url = "http://www.example1.com";
        calendarEvent1.date = 2065938828000L;
        CalendarEvent calendarEvent2 = new CalendarEvent();
        calendarEvent2.id = "2";
        calendarEvent2.name = "Event 2";
        calendarEvent2.description = "Event 2 - In Favourite";
        calendarEvent2.url = "http://www.example2.com";
        calendarEvent2.date = 2065938828000L;

        eventsRepository.indexOrUpdate(calendarEvent1);
        eventsRepository.indexOrUpdate(calendarEvent2);

        usersRepository.save(user);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDownOne() {
        ElasticUtils.deleteIndex();
    }

    @Test
    public void testGetListItems() {
        Assertions.assertThat(usersRepository.getListItems(user,
                UsersRepository.FavouriteItem.FavouriteType.TAG))
                .isEqualTo(user.favourites.tags);
        Assertions.assertThat(usersRepository.getListItems(user,
                UsersRepository.FavouriteItem.FavouriteType.CITY))
                .isEqualTo(user.favourites.cities);
        Assertions.assertThat(usersRepository.getListItems(user,
                UsersRepository.FavouriteItem.FavouriteType.CONFERENCE))
                .isEqualTo(user.favourites.conferences);
        Assertions.assertThat(usersRepository.getListItems(user,
                UsersRepository.FavouriteItem.FavouriteType.COMMUNITY))
                .isEqualTo(user.favourites.communities);
        Assertions.assertThat(usersRepository.getListItems(user,
                UsersRepository.FavouriteItem.FavouriteType.CALENDAR))
                .isEqualTo(user.favourites.upcomingEvents);
    }

    @Test
    public void testFavourite() {
        Assertions.assertThat(user.favourites.tags.size()).isEqualTo(0);

        // Add favourite
        usersRepository.addFavourite(user, UsersRepository.FavouriteItem.FavouriteType.TAG, "Ruben");

        Assertions.assertThat(user.favourites.tags.size()).isEqualTo(1);
        Assertions.assertThat(user.favourites.tags.get(0)).matches("Ruben");

        // remove favourite
        usersRepository.removeFavourite(user, UsersRepository.FavouriteItem.FavouriteType.TAG, "Ruben");
        Assertions.assertThat(user.favourites.tags.size()).isEqualTo(0);


        // Search cCalendarEvents which match with a favourite
        usersRepository.addFavourite(user, UsersRepository.FavouriteItem.FavouriteType.CALENDAR, "1");
        usersRepository.addFavourite(user, UsersRepository.FavouriteItem.FavouriteType.CALENDAR, "2");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        SimpleSearchResult simpleSearchResult = usersRepository.getFavourites(user, UsersRepository.FavouriteItem.FavouriteType.CALENDAR);
        Assertions.assertThat(simpleSearchResult.query).matches("favourites/CALENDAR");
        Assertions.assertThat(simpleSearchResult.hits.size()).isEqualTo(2);
        Assertions.assertThat(simpleSearchResult.hits.get(0)).isInstanceOf(CalendarEvent.class);
        Assertions.assertThat(((CalendarEvent) simpleSearchResult.hits.get(0)).name).isEqualTo("Event 1");

        // Other types
        simpleSearchResult = usersRepository.getFavourites(user, UsersRepository.FavouriteItem.FavouriteType.CONFERENCE);
        Assertions.assertThat(simpleSearchResult.query).matches("favourites/CONFERENCE");
        simpleSearchResult = usersRepository.getFavourites(user, UsersRepository.FavouriteItem.FavouriteType.COMMUNITY);
        Assertions.assertThat(simpleSearchResult.query).matches("favourites/COMMUNITY");

        // Default : exception !
        try {
            simpleSearchResult = usersRepository.getFavourites(user, UsersRepository.FavouriteItem.FavouriteType.TAG);

            Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
        } catch (RuntimeException e) {
            Assertions.assertThat(e.getMessage()).isEqualTo("HTML 400 : Unsupported FavouriteType : TAG");
        }

        // user == null => null
        simpleSearchResult = usersRepository.getFavourites(null, UsersRepository.FavouriteItem.FavouriteType.TAG);
        Assertions.assertThat(simpleSearchResult).isNull();

    }
}
