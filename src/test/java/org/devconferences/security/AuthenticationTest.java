package org.devconferences.security;

import net.codestory.http.Context;
import net.codestory.http.Cookie;
import net.codestory.http.Cookies;
import net.codestory.http.constants.HttpStatus;
import net.codestory.http.payload.Payload;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Response;
import org.assertj.core.api.Assertions;
import org.devconferences.elastic.DeveloppementESNode;
import org.devconferences.elastic.MockJestClient;
import org.devconferences.elastic.RuntimeJestClientAdapter;
import org.devconferences.users.User;
import org.devconferences.users.UsersRepository;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by chris on 10/06/15.
 */
public class AuthenticationTest {
    private final RuntimeJestClientAdapter mockClient = mock(RuntimeJestClientAdapter.class);
    private final Authentication authentication = new Authentication(new Encrypter(), new UsersRepository(mockClient, null));
    private final Encrypter encrypter  = new Encrypter();

    @Test
    public void testExtractUser() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("login", "login");
        map.put("id", "id");
        map.put("avatar_url", "http://avatar_url");

        User user = authentication.extractUserFromResponse(map);

        assertEquals("login", user.login);
        assertEquals("id", user.id);
        assertEquals("http://avatar_url", user.avatarURL);
        assertNull(user.email);
    }

    @Test
    public void testConnect() {
        DeveloppementESNode.deleteDevNode();
        DeveloppementESNode.setPortNode("9450");
        GithubCalls mockGithub = mock(GithubCalls.class);
        Response responseUser = mock(Response.class);
        Content contentMock = mock(Content.class);
        Context context = mock(Context.class);
        Cookie cookie = mock(Cookie.class);
        Cookies cookies = mock(Cookies.class);
        Authentication.GitHubAuthenticationResponse githubResponse = authentication.new GitHubAuthenticationResponse();
        githubResponse.accessToken = "azoekmv646ZEKMKL51aze";
        String githubUser = "{" +
                "  \"login\": \"user123\"," +
                "  \"id\": 1234567," +
                "  \"avatar_url\": \"http://my-avatar.com\"," +
                "  \"email\": \"foo@devconferences.org\"" +
                "}";

        when(context.cookies()).thenReturn(cookies);

        // IsAuthenticated should return false
        try {
            when(cookies.get("access_token")).thenReturn(null);
            Assertions.assertThat(authentication.isAuthenticated(context)).isFalse();
        } catch (IOException e) {
            e.printStackTrace();
        }


        when(cookies.get("access_token")).thenReturn(cookie);
        when(cookie.name()).thenReturn("access_token");
        when(cookie.value()).thenReturn(encrypter.encrypt(githubResponse.accessToken));

        // Try to authorize connection
        try {
            when(contentMock.asString()).thenReturn(githubUser);
            when(responseUser.returnContent()).thenReturn(contentMock);
            when(mockGithub.getUser(githubResponse.accessToken, true)).thenReturn(responseUser);
            when(mockGithub.authorize("a1b2c3d4e5f67890")).thenReturn(githubResponse);
            MockJestClient.configGet(mockClient, githubUser);

        } catch (IOException e) {
            e.printStackTrace();
        }

        Authentication authenticationWithMock = new Authentication(new Encrypter(), new UsersRepository(mockClient, null), mockGithub);

        Payload payloadConnect = authenticationWithMock.oauthCallBack("a1b2c3d4e5f67890", context);

        Assertions.assertThat(payloadConnect.cookies().get(0).name()).matches("access_token");
        Assertions.assertThat(encrypter.decrypt(payloadConnect.cookies().get(0).value())).matches(githubResponse.accessToken);

        // IsAuthenticated should return true
        try {
            HttpResponse httpResponse = mock(HttpResponse.class);
            StatusLine statusLine = mock(StatusLine.class);

            when(mockGithub.getUser(githubResponse.accessToken, false)).thenReturn(responseUser);
            when(responseUser.returnResponse()).thenReturn(httpResponse);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(HttpStatus.OK);

            Assertions.assertThat(authenticationWithMock.isAuthenticated(context)).isTrue();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assertions.assertThat(authenticationWithMock.getConnectedUser(context).login).matches("user123");
        Assertions.assertThat(authenticationWithMock.getConnectedUser(context).id).matches("1234567.0");

        Assertions.assertThat(authenticationWithMock.getUser(context).login).matches("user123");
        Assertions.assertThat(authenticationWithMock.getUser(context).id).matches("1234567.0");

        // Try to disconnect
        Payload payloadDisconnect = authenticationWithMock.disconnect(context);

        Assertions.assertThat(payloadDisconnect.cookies().get(0).name()).matches("access_token");
        Assertions.assertThat(payloadDisconnect.cookies().get(0).value()).isNull();
        Assertions.assertThat(payloadDisconnect.cookies().get(0).expiry()).isEqualTo(1);
    }
}
