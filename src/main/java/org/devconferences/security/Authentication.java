package org.devconferences.security;

import com.google.gson.Gson;
import com.google.inject.Inject;
import io.searchbox.core.DocumentResult;
import net.codestory.http.Context;
import net.codestory.http.Cookie;
import net.codestory.http.NewCookie;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.errors.BadRequestException;
import net.codestory.http.errors.NotFoundException;
import net.codestory.http.payload.Payload;
import org.apache.http.client.fluent.Content;
import org.devconferences.events.search.SimpleSearchResult;
import org.devconferences.users.User;
import org.devconferences.users.UsersRepository;

import java.io.IOException;
import java.util.Map;

/**
 * Created by chris on 05/06/15.
 */
@Prefix("auth/")
public class Authentication {
    public static final String ACCESS_TOKEN = "access_token";

    private final Encrypter encrypter;
    private final UsersRepository usersRepository;
    private final GithubCalls githubCalls;

    private final Gson gson = new Gson();

    @Inject
    public Authentication(Encrypter encrypter, UsersRepository usersRepository) {
        this(encrypter, usersRepository, new GithubCalls());
    }

    public Authentication(Encrypter encrypter, UsersRepository usersRepository, GithubCalls githubCalls) {
        this.githubCalls = githubCalls;
        this.encrypter = encrypter;
        this.usersRepository = usersRepository;
    }

    @Get("?code=:code")
    public Payload oauthCallBack(String code, Context context) {
        if(code != null) {
            try {
                GitHubAuthenticationResponse authenticationResponse = githubCalls.authorize(code);

                User user = getUser(authenticationResponse.accessToken);
                usersRepository.save(user);
                context.setCurrentUser(user);

                NewCookie accessTokenCookie = new NewCookie(ACCESS_TOKEN, encrypter.encrypt(authenticationResponse.accessToken), true);
                //TODO move to secure (HTTPS only) newCookie.setSecure(true);
                return Payload.seeOther("/")
                        .withCookie(accessTokenCookie);
                //.withCookie("user", gson.toJson(user));
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            // URI : /auth/
            throw new NotFoundException();
        }
    }

    @Get("disconnect")
    public Payload disconnect(Context context) {
        context.setCurrentUser(null);
        NewCookie disconnectCookie = new NewCookie(ACCESS_TOKEN, encrypter.encrypt("0"), true).setExpiry(1);
        return Payload.seeOther("/").withCookie(disconnectCookie);
    }

    @Get("connected-user")
    public User getConnectedUser(Context context) {
        User user = getUser(context);
        return NotFoundException.notFoundIfNull(user);
    }

    public User getUser(Context context) {
        String accessToken = extractAccessToken(context);
        return getUser(accessToken);
    }

    @Get("client-id")
    public String getClientId() {
        return githubCalls.clientId;
    }

    @Get("favourites/:type")
    public SimpleSearchResult getFavouritesPerType(String type, Context context) {
        try {
            UsersRepository.FavouriteItem.FavouriteType typeEnum = UsersRepository.FavouriteItem.FavouriteType.valueOf(type);
            return usersRepository.getFavourites(getUser(context), typeEnum);
        } catch(RuntimeException e) {
            if(e.getMessage().startsWith("HTML 400 :") || e.getMessage().startsWith("No enum constant ")) {
                throw new BadRequestException();
            } else {
                throw e;
            }
        }
    }

    @Post("favourites")
    public void addFavourite(UsersRepository.FavouriteItem item, Context context) {
        DocumentResult documentResult = usersRepository.addFavourite(getUser(context), item.type, item.value);

        if(!documentResult.isSucceeded()) {
            throw new RuntimeException(documentResult.getErrorMessage());
        }
    }

    @Delete("favourites/:type/:value?filter=:filter")
    public void removeFavourite(String type, String value, String filter, Context context) {
        UsersRepository.FavouriteItem.FavouriteType typeEnum =
                UsersRepository.FavouriteItem.FavouriteType.valueOf(type);
        String favourite = (filter != null ? value + "/" + filter : value);
        DocumentResult documentResult = usersRepository.removeFavourite(getUser(context), typeEnum, favourite);

        if(!documentResult.isSucceeded()) {
            throw new RuntimeException(documentResult.getErrorMessage());
        }
    }

    @Delete("messages/:id")
    public void deleteMessage(String id, Context context) {
        DocumentResult documentResult = usersRepository.deleteMessage(getUser(context), id);

        if(!documentResult.isSucceeded()) {
            throw new RuntimeException(documentResult.getErrorMessage());
        }
    }

    public boolean isAuthenticated(Context context) throws IOException {
        String accessToken = extractAccessToken(context);
        if(accessToken == null) {
            return false;
        }

        int statusCode = githubCalls.getUser(accessToken, false)
                .returnResponse()
                .getStatusLine()
                .getStatusCode();
        return statusCode == HttpStatus.OK;
    }

    private User getUser(String accessToken) {
        if(accessToken == null) {
            return null;
        }
        try {
            Content content = githubCalls.getUser(accessToken, true)
                    .returnContent();

            Map<String, Object> map = gson.fromJson(content.asString(), Map.class);
            User userFromResponse = extractUserFromResponse(map);
            if(userFromResponse == null) {
                return null;
            }

            User user = usersRepository.getUser(userFromResponse.login);
            if(user == null) {
                user = userFromResponse;
            }
            user.avatarURL = userFromResponse.avatarURL;
            user.email = userFromResponse.email;
            user.login = userFromResponse.login;
            return user;

        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    User extractUserFromResponse(Map<String, Object> map) {
        String login = (String) map.get("login");
        String id = map.get("id").toString();
        String avatarUrl = (String) map.get("avatar_url");
        String email = (String) map.get("email");

        User user = new User(login, id, email, avatarUrl);
        return user;
    }

    private String extractAccessToken(Context context) {
        if(context == null) {
            return null;
        }
        Cookie cookie = context.cookies().get(ACCESS_TOKEN);
        return cookie != null ? encrypter.decrypt(cookie.value()) : null;
    }
}
