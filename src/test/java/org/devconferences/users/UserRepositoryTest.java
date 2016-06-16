package org.devconferences.users;

import org.assertj.core.api.Assertions;
import org.devconferences.elastic.MockJestClient;
import org.devconferences.elastic.RuntimeJestClient;
import org.devconferences.elastic.RuntimeJestClientAdapter;
import org.devconferences.events.CalendarEvent;
import org.devconferences.events.EventsRepository;
import org.devconferences.events.search.SimpleSearchResult;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class UserRepositoryTest {
    private RuntimeJestClientAdapter mockClient;
    private UsersRepository usersRepository;
    private EventsRepository eventsRepository;
    private User user;

    @Before
    public void setUp() {
        mockClient = mock(RuntimeJestClientAdapter.class);

        eventsRepository = new EventsRepository(mockClient);
        usersRepository = new UsersRepository(mockClient, eventsRepository);
        user = new User("abc1234", "1243012.0", "foo@devconferences.org", "/img/no_logo.png");
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

        // Get favourite for a type
        String searchByIds = "[" +
                "  {" +
                "    \"_index\" : \"dev-conferences\"," +
                "    \"_type\" : \"calendarevents\"," +
                "    \"_id\" : \"1\"," +
                "    \"_source\" : {" +
                "        \"id\" : \"1\"," +
                "        \"name\" : \"Event 1 - In Favourite\"," +
                "        \"description\" : \"Event 1\"," +
                "        \"url\" : \"http://www.example.com\"" +
                "    }" +
                "  }," +
                "  {" +
                "    \"_index\" : \"dev-conferences\"," +
                "    \"_type\" : \"calendarevents\"," +
                "    \"_id\" : \"2\"," +
                "    \"_source\" : {" +
                "        \"id\" : \"2\"," +
                "        \"name\" : \"Event 2\"," +
                "        \"description\" : \"Event 2- In favourite\"," +
                "        \"url\" : \"http://www.example2.com\"" +
                "    }" +
                "  }" +
                "]";
        MockJestClient.configSearch(mockClient, 2, searchByIds, "{}");
        SimpleSearchResult simpleSearchResult = usersRepository.getFavourites(user, UsersRepository.FavouriteItem.FavouriteType.CALENDAR);
        Assertions.assertThat(simpleSearchResult.query).matches("favourites/CALENDAR");
        Assertions.assertThat(simpleSearchResult.hits.size()).isEqualTo(2);
        Assertions.assertThat(simpleSearchResult.hits.get(0)).isInstanceOf(CalendarEvent.class);
        Assertions.assertThat(((CalendarEvent) simpleSearchResult.hits.get(0)).name).isEqualTo("Event 1 - In Favourite");

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
