package org.devconferences.users;

import net.codestory.http.Context;
import net.codestory.http.Cookie;
import net.codestory.http.Cookies;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Response;
import org.assertj.core.api.Assertions;
import org.devconferences.elastic.*;
import org.devconferences.events.CalendarEvent;
import org.devconferences.events.EventsRepository;
import org.devconferences.events.search.SimpleSearchResult;
import org.devconferences.security.Authentication;
import org.devconferences.security.Encrypter;
import org.devconferences.security.GitHubAuthenticationResponse;
import org.devconferences.security.GithubCalls;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserRepositoryTest {
    private static UsersRepository usersRepository;
    private static EventsRepository eventsRepository;
    private static Authentication authentication;
    private static User user;
    private static Context contextMock;

    @BeforeClass
    public static void classSetUp() {
        DeveloppementESNode.createDevNode("9250");
        ElasticUtils.createIndex();

        eventsRepository = new EventsRepository();
        usersRepository = new UsersRepository(ElasticUtils.createClient(), eventsRepository);
        authentication = new Authentication(new Encrypter(), usersRepository, configGithubCalls());
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

        contextMock = configContext();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static GithubCalls configGithubCalls() {
        GithubCalls mockGithub = mock(GithubCalls.class);
        Response responseUser = mock(Response.class);
        Content contentMock = mock(Content.class);
        Context context = mock(Context.class);
        Cookie cookie = mock(Cookie.class);
        Cookies cookies = mock(Cookies.class);
        GitHubAuthenticationResponse githubResponse = new GitHubAuthenticationResponse();
        githubResponse.accessToken = "azoekmv646ZEKMKL51aze";
        String githubUser = "{" +
                "  \"login\": \"abc1234\"," +
                "  \"id\": 1243012," +
                "  \"avatar_url\": \"/img/no_logo.png\"," +
                "  \"email\": \"foo@devconferences.org\"" +
                "}";

        when(context.cookies()).thenReturn(cookies);
        when(cookies.get("access_token")).thenReturn(cookie);
        when(cookie.name()).thenReturn("access_token");
        when(cookie.value()).thenReturn(new Encrypter().encrypt(githubResponse.accessToken));

        // Try to authorize connection
        try {
            when(contentMock.asString()).thenReturn(githubUser);
            when(responseUser.returnContent()).thenReturn(contentMock);
            when(mockGithub.getUser(githubResponse.accessToken, true)).thenReturn(responseUser);
            when(mockGithub.authorize("a1b2c3d4e5f67890")).thenReturn(githubResponse);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return mockGithub;
    }

    private static Context configContext() {
        Context context = mock(Context.class);
        Cookies cookies = mock(Cookies.class);
        Cookie cookie = mock(Cookie.class);

        String accessToken = "azoekmv646ZEKMKL51aze";
        when(context.cookies()).thenReturn(cookies);

        when(cookies.get("access_token")).thenReturn(cookie);
        when(cookie.name()).thenReturn("access_token");
        when(cookie.value()).thenReturn(new Encrypter().encrypt(accessToken));

        return context;
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
        UsersRepository.FavouriteItem favourite = new UsersRepository.FavouriteItem();
        favourite.type = UsersRepository.FavouriteItem.FavouriteType.TAG;
        favourite.value = "Ruben";
        authentication.addFavourite(favourite, contextMock);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        User user1 = authentication.getUser(contextMock);
        Assertions.assertThat(user1.favourites.tags.size()).isEqualTo(1);
        Assertions.assertThat(user1.favourites.tags.get(0)).matches("Ruben");
        Assertions.assertThat(authentication.getFavouritesPerType("CONFERENCE", contextMock).hits).hasSize(0);
        Assertions.assertThat(authentication.getFavouritesPerType("COMMUNITY", contextMock).hits).hasSize(0);

        // remove favourite
        authentication.removeFavourite(favourite.type.toString(), favourite.value, null, contextMock);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        user1 = authentication.getUser(contextMock);
        Assertions.assertThat(user1.favourites.tags.size()).isEqualTo(0);


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
