package org.devconferences.users;

import org.assertj.core.api.Assertions;
import org.devconferences.elastic.RuntimeJestClient;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class UserRepositoryTest {
    private UsersRepository usersRepository;
    private User user;

    @Before
    public void setUp() {
        RuntimeJestClient mockClient = mock(RuntimeJestClient.class);

        usersRepository = new UsersRepository(mockClient);
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

        usersRepository.addFavourite(user, UsersRepository.FavouriteItem.FavouriteType.TAG, "Ruben");

        Assertions.assertThat(user.favourites.tags.size()).isEqualTo(1);
        Assertions.assertThat(user.favourites.tags.get(0)).matches("Ruben");

        usersRepository.removeFavourite(user, UsersRepository.FavouriteItem.FavouriteType.TAG, "Ruben");

        Assertions.assertThat(user.favourites.tags.size()).isEqualTo(0);
    }
}
