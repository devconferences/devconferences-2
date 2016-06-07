package org.devconferences.security;

import net.codestory.http.Context;
import net.codestory.http.misc.Env;
import net.codestory.http.payload.Payload;
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
    private final Authentication authentication = new Authentication(new Encrypter(), new UsersRepository(mockClient));
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
        DeveloppementESNode.setPortNode("9450");
        GithubCalls mockGithub = mock(GithubCalls.class);
        Authentication.GitHubAuthenticationResponse githubResponse = authentication.new GitHubAuthenticationResponse();
        githubResponse.accessToken = "azoekmv646ZEKMKL51aze";
        Response responseUser = mock(Response.class);
        Content contentMock = mock(Content.class);
        String githubUser = "{" +
                "  \"login\": \"user123\"," +
                "  \"id\": 1234567," +
                "  \"avatar_url\": \"http://my-avatar.com\"," +
                "  \"email\": \"foo@devconferences.org\"" +
                "}";
        String stringGet = githubUser;
        try {
            when(contentMock.asString()).thenReturn(githubUser);
            when(responseUser.returnContent()).thenReturn(contentMock);
            when(mockGithub.getUser(githubResponse.accessToken, true)).thenReturn(responseUser);
            when(mockGithub.authorize("a1b2c3d4e5f67890")).thenReturn(githubResponse);
            MockJestClient.configGet(mockClient, UsersRepository.USERS_TYPE, stringGet);

        } catch (IOException e) {
            e.printStackTrace();
        }

        Authentication authenticationWithMock = new Authentication(new Encrypter(), new UsersRepository(mockClient), mockGithub);

        Payload payload = authenticationWithMock.oauthCallBack("a1b2c3d4e5f67890", new Context(null, null, null, null, null));

        Assertions.assertThat(payload.cookies().get(0).name()).matches("access_token");
        Assertions.assertThat(encrypter.decrypt(payload.cookies().get(0).value())).matches(githubResponse.accessToken);
    }
}
