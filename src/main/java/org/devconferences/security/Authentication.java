package org.devconferences.security;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import net.codestory.http.Context;
import net.codestory.http.Cookie;
import net.codestory.http.Cookies;
import net.codestory.http.NewCookie;
import net.codestory.http.annotations.Delete;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.constants.Headers;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.devconferences.users.UsersRepository;
import org.devconferences.users.User;

import java.io.IOException;
import java.util.Map;

import static org.devconferences.env.EnvUtils.fromEnv;

/**
 * Created by chris on 05/06/15.
 */
@Prefix("auth/")
public class Authentication {
    public static final String GITHUB_OAUTH_CLIENT_ID = "GITHUB_OAUTH_CLIENT_ID";
    public static final String GITHUB_OAUTH_CLIENT_SECRET = "GITHUB_OAUTH_CLIENT_SECRET";
    public static final String ACCESS_TOKEN = "access_token";

    private final String clientId;
    private final String clientSecret;

    private final Encrypter encrypter;
    private final UsersRepository usersRepository;

    private final Gson gson = new Gson();

    @Inject
    public Authentication(Encrypter encrypter, UsersRepository usersRepository) {
        clientId = fromEnv(GITHUB_OAUTH_CLIENT_ID, "9a8a7843de53c0561a73");
        clientSecret = fromEnv(GITHUB_OAUTH_CLIENT_SECRET, "64b3cb1d323b0ef17aa5f0390e4dde88c8ec42a0");

        this.encrypter = encrypter;
        this.usersRepository = usersRepository;
    }

    @Get("?code=:code")
    public Payload oauthCallBack(String code) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build();) {
            Content content = Request.Post("https://github.com/login/oauth/access_token")
                    .bodyForm(Form.form()
                            .add("client_id", clientId)
                            .add("client_secret", clientSecret)
                            .add("code", code)
                            .build())
                    .addHeader(new BasicHeader(Headers.ACCEPT, "application/json"))
                    .execute()
                    .returnContent();
            GitHubAuthenticationResponse authenticationResponse = gson.fromJson(content.asString(), GitHubAuthenticationResponse.class);

            User user = getUser(authenticationResponse.accessToken);
            usersRepository.save(user);

            NewCookie newCookie = new NewCookie(ACCESS_TOKEN, encrypter.encrypt(authenticationResponse.accessToken), true);
            //TODO move to secure (HTTPS only) newCookie.setSecure(true);
            return Payload.seeOther("/")
                    .withCookie(newCookie)
                    .withCookie("user", gson.toJson(user));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Get("disconnect")
    public Payload disconnect(Context context){
        return Payload.seeOther("/");
    }

    @Get("connected-user")
    public User getUser(Context context) {
        String accessToken = extractAccessToken(context);
        return getUser(accessToken);
    }

    @Get("client-id")
    public String getClientId(){
        return clientId;
    }

    public boolean isAuthenticated(Context context) throws IOException {
        String accessToken = extractAccessToken(context);
        if (accessToken == null) {
            return false;
        }

        int statusCode = Request.Head("https://api.github.com/user?access_token=" + accessToken)
                .execute()
                .returnResponse()
                .getStatusLine()
                .getStatusCode();
        return statusCode == HttpStatus.OK;
    }

    private User getUser(String accessToken) {
        try {
            Content content = Request.Get("https://api.github.com/user?access_token=" + accessToken)
                    .execute()
                    .returnContent();

            Map<String, Object> map = gson.fromJson(content.asString(), Map.class);
            User userFromResponse = extractUserFromResponse(map);
            if (userFromResponse == null) {
                return null;
            }

            User user = usersRepository.getUser(userFromResponse.id);
            if (user == null) {
                user = userFromResponse;
            }
            user.avatarURL = userFromResponse.avatarURL;
            user.email = userFromResponse.email;
            user.login = userFromResponse.login;
            return user;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private User extractUserFromResponse(Map<String, Object> map) {
        String login = map.get("login").toString();
        String id = map.get("id").toString();
        String avatarUrl = map.get("avatar_url").toString();
        String email = map.get("email").toString();

        User user = new User(login, id, email, avatarUrl);
        return user;
    }

    private String extractAccessToken(Context context) {
        Cookie cookie = context.cookies().get(ACCESS_TOKEN);
        return cookie != null ? encrypter.decrypt(cookie.value()) : null;
    }

    private class GitHubAuthenticationResponse {
        @SerializedName("access_token")
        public String accessToken;
        @SerializedName("token_type")
        public String tokenType;
        public String scope;
    }
}
