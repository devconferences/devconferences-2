package org.devconferences.users;

import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.indices.Refresh;
import net.codestory.http.Context;
import net.codestory.http.Cookie;
import net.codestory.http.Cookies;
import net.codestory.http.errors.BadRequestException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Response;
import org.assertj.core.api.Assertions;
import org.devconferences.elastic.*;
import org.devconferences.events.CalendarEvent;
import org.devconferences.events.Event;
import org.devconferences.events.EventsRepository;
import org.devconferences.events.search.SimpleSearchResult;
import org.devconferences.security.Authentication;
import org.devconferences.security.Encrypter;
import org.devconferences.security.GitHubAuthenticationResponse;
import org.devconferences.security.GithubCalls;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.devconferences.elastic.ElasticUtils.DEV_CONFERENCES_INDEX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserRepositoryTest {
    private static UsersRepository usersRepository;
    private static EventsRepository eventsRepository;
    private static Authentication authentication;
    private static User user;
    private static Context contextMock;

    private static Event event1;
    private static Event event2;
    private static Event event3;
    private static Event event4;
    private static CalendarEvent calendarEvent1;
    private static CalendarEvent calendarEvent2;

    @BeforeClass
    public static void classSetUp() {
        DeveloppementESNode.createDevNode("9250");
        ElasticUtils.createIndex();

        eventsRepository = new EventsRepository();
        usersRepository = new UsersRepository(ElasticUtils.createClient(), eventsRepository);
        authentication = new Authentication(new Encrypter(), usersRepository, configGithubCalls());
        user = new User("abc1234", "1243012.0", "foo@devconferences.org", "/img/no_logo.png");

        event1 = new Event();
        event1.id = "2";
        event1.name = "Cigale 42";
        event1.city = "City 1";
        event1.type = Event.Type.CONFERENCE;
        event2 = new Event();
        event2.id = "3";
        event2.name = "Event 3";
        event2.city = "City 1";
        event2.type = Event.Type.COMMUNITY;
        event3 = new Event();
        event3.id = "5";
        event3.name = "Event 5";
        event3.city = "City 1";
        event3.type = Event.Type.CONFERENCE;
        event4 = new Event();
        event4.id = "7";
        event4.name = "Event 7";
        event4.city = "City 2";
        event4.type = Event.Type.COMMUNITY;

        eventsRepository.indexOrUpdate(event1);
        eventsRepository.indexOrUpdate(event2);
        eventsRepository.indexOrUpdate(event3);

        calendarEvent1 = new CalendarEvent();
        calendarEvent1.id = "1";
        calendarEvent1.name = "Event 1";
        calendarEvent1.description = "Event 1 - In Favourite";
        calendarEvent1.url = "http://www.example1.com";
        calendarEvent1.date = 2065938828000L;
        calendarEvent2 = new CalendarEvent();
        calendarEvent2.id = "2";
        calendarEvent2.name = "Event 2";
        calendarEvent2.description = "Event 2 - In Favourite";
        calendarEvent2.url = "http://www.example2.com";
        calendarEvent2.date = 2065938828000L;

        eventsRepository.indexOrUpdate(calendarEvent1);
        eventsRepository.indexOrUpdate(calendarEvent2);

        usersRepository.save(user);

        Refresh refresh = new Refresh.Builder().addIndex(DEV_CONFERENCES_INDEX).build();
        ElasticUtils.createClient().execute(refresh);

        contextMock = configContext();
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
        ElasticUtils.deleteAllTypes();
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
        Refresh refresh = new Refresh.Builder().addIndex(DEV_CONFERENCES_INDEX).build();
        RuntimeJestClient client = ElasticUtils.createClient();

        Assertions.assertThat(user.favourites.tags.size()).isEqualTo(0);

        // Add favourite
        UsersRepository.FavouriteItem favourite = new UsersRepository.FavouriteItem();
        favourite.type = UsersRepository.FavouriteItem.FavouriteType.TAG;
        favourite.value = "Ruben";
        authentication.addFavourite(favourite, contextMock);

        client.execute(refresh);

        User user1 = authentication.getUser(contextMock);
        Assertions.assertThat(user1.favourites.tags.size()).isEqualTo(1);
        Assertions.assertThat(user1.favourites.tags.get(0)).matches("Ruben");
        Assertions.assertThat(authentication.getFavouritesPerType("CONFERENCE", contextMock).hits).hasSize(0);
        Assertions.assertThat(authentication.getFavouritesPerType("COMMUNITY", contextMock).hits).hasSize(0);
        try {
            authentication.getFavouritesPerType("TAG", contextMock);

            Assertions.failBecauseExceptionWasNotThrown(BadRequestException.class);
        } catch(BadRequestException e) {
            // GOOD !
        }

        // remove favourite
        authentication.removeFavourite(favourite.type.toString(), favourite.value, null, contextMock);

        client.execute(refresh);

        user1 = authentication.getUser(contextMock);
        Assertions.assertThat(user1.favourites.tags.size()).isEqualTo(0);


        // Search cCalendarEvents which match with a favourite
        usersRepository.addFavourite(user, UsersRepository.FavouriteItem.FavouriteType.CALENDAR, "1");
        usersRepository.addFavourite(user, UsersRepository.FavouriteItem.FavouriteType.CALENDAR, "2");

        client.execute(refresh);

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

        // tearDown
        usersRepository.removeFavourite(user, UsersRepository.FavouriteItem.FavouriteType.CALENDAR, "1");
        usersRepository.removeFavourite(user, UsersRepository.FavouriteItem.FavouriteType.CALENDAR, "2");

    }

    @Test
    public void testNotifications() {
        RuntimeJestClient client = ElasticUtils.createClient();
        Refresh refresh = new Refresh.Builder().addIndex(DEV_CONFERENCES_INDEX).build();
        if(client == null) {
            Assertions.fail("Client should be created !");
        }
        // Add some favourites
        usersRepository.addFavourite(user, UsersRepository.FavouriteItem.FavouriteType.CALENDAR, "1");
        usersRepository.addFavourite(user, UsersRepository.FavouriteItem.FavouriteType.COMMUNITY, "3");
        usersRepository.addFavourite(user, UsersRepository.FavouriteItem.FavouriteType.CONFERENCE, "5");
        usersRepository.addFavourite(user, UsersRepository.FavouriteItem.FavouriteType.CITY, "City 2");
        usersRepository.addFavourite(user, UsersRepository.FavouriteItem.FavouriteType.TAG, "Cigale");
        client.execute(refresh);

        // Test percolators
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery()).sort(SortBuilders.fieldSort("_id"));
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(DEV_CONFERENCES_INDEX).addType(".percolator").build();

        SearchResult searchResult = client.execute(search);
        List<SearchResult.Hit<PercolateResponse,Void>> listHits = searchResult.getHits(PercolateResponse.class);
        Assertions.assertThat(listHits).hasSize(6);
        Assertions.assertThat(listHits.get(0).sort.get(0)).matches("abc1234_CALENDAR_1");
        Assertions.assertThat(listHits.get(1).sort.get(0)).matches("abc1234_CITY_City 2");
        Assertions.assertThat(listHits.get(2).sort.get(0)).matches("abc1234_CITY_City 2_geo"); // City 2 have geohash
        Assertions.assertThat(listHits.get(3).sort.get(0)).matches("abc1234_COMMUNITY_3");
        Assertions.assertThat(listHits.get(4).sort.get(0)).matches("abc1234_CONFERENCE_5");
        Assertions.assertThat(listHits.get(5).sort.get(0)).matches("abc1234_TAG_Cigale");

        // Test some updates
        Event eventUpdate1 = new Event(event1);
        eventUpdate1.description = "Hello World";
        eventsRepository.indexOrUpdate(eventUpdate1);

        client.execute(refresh);

        Event eventUpdate2 = new Event(event2);
        eventUpdate2.description = "Hello World";
        eventsRepository.indexOrUpdate(eventUpdate2);

        client.execute(refresh);

        Event eventUpdate3 = new Event(event3);
        eventUpdate3.description = "Hello World";
        eventsRepository.indexOrUpdate(eventUpdate3);

        client.execute(refresh);

        Event eventUpdate4 = new Event(event4);
        eventUpdate4.description = "Hello World";
        eventsRepository.indexOrUpdate(eventUpdate4);

        client.execute(refresh);

        CalendarEvent calendarEventUpdate1 = new CalendarEvent(calendarEvent1);
        calendarEventUpdate1.description = "Hello World";
        eventsRepository.indexOrUpdate(calendarEventUpdate1);

        client.execute(refresh);

        CalendarEvent calendarEventUpdate2 = new CalendarEvent(calendarEvent2);
        calendarEventUpdate2.description = "Hello World";
        eventsRepository.indexOrUpdate(calendarEventUpdate2);

        client.execute(refresh);

        User user1 = authentication.getUser(contextMock);
        Assertions.assertThat(user1.messages).hasSize(5);
        Assertions.assertThat(user1.messages.get(0).text).matches("Une conférence pouvant vous intéresser a été mise à jour : Cigale 42");
        Assertions.assertThat(user1.messages.get(1).text).matches("Une communauté favorite a été mise à jour : Event 3");
        Assertions.assertThat(user1.messages.get(2).text).matches("Une conférence favorite a été mise à jour : Event 5");
        Assertions.assertThat(user1.messages.get(3).text).matches("Une communauté dans une ville favorite a été créée : Event 7");
        Assertions.assertThat(user1.messages.get(4).text).matches("Un événement favori a été mis à jour : Event 1");

        // Read messages
        user1.messages.forEach(message -> {
            authentication.deleteMessage(message.id, contextMock);
            client.execute(refresh);
        });

        Assertions.assertThat(authentication.getUser(contextMock).messages).hasSize(0);

        // TearDown
        usersRepository.removeFavourite(user, UsersRepository.FavouriteItem.FavouriteType.CALENDAR, "1");
        usersRepository.removeFavourite(user, UsersRepository.FavouriteItem.FavouriteType.COMMUNITY, "3");
        usersRepository.removeFavourite(user, UsersRepository.FavouriteItem.FavouriteType.CONFERENCE, "5");
        usersRepository.removeFavourite(user, UsersRepository.FavouriteItem.FavouriteType.CITY, "City 2");
        usersRepository.removeFavourite(user, UsersRepository.FavouriteItem.FavouriteType.TAG, "Cigale");
        client.execute(refresh);
    }
}
