package org.devconferences.security;

import com.google.gson.Gson;
import net.codestory.http.constants.Headers;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.message.BasicHeader;

import java.io.IOException;

import static org.devconferences.env.EnvUtils.fromEnv;

public class GithubCalls {
    public static final String GITHUB_OAUTH_CLIENT_ID = "GITHUB_OAUTH_CLIENT_ID";
    public static final String GITHUB_OAUTH_CLIENT_SECRET = "GITHUB_OAUTH_CLIENT_SECRET";

    final String clientId;
    private final String clientSecret;

    private final Gson gson = new Gson();

    public GithubCalls() {
        clientId = fromEnv(GITHUB_OAUTH_CLIENT_ID, "9a8a7843de53c0561a73");
        clientSecret = fromEnv(GITHUB_OAUTH_CLIENT_SECRET, "64b3cb1d323b0ef17aa5f0390e4dde88c8ec42a0");
    }

    public GitHubAuthenticationResponse authorize(String code) throws IOException {
        Content content = Request.Post("https://github.com/login/oauth/access_token")
                .bodyForm(Form.form()
                        .add("client_id", clientId)
                        .add("client_secret", clientSecret)
                        .add("code", code)
                        .build())
                .addHeader(new BasicHeader(Headers.ACCEPT, "application/json"))
                .execute()
                .returnContent();
        return gson.fromJson(content.asString(), GitHubAuthenticationResponse.class);
    }

    public Response getUser(String accessToken, boolean withBody) throws IOException {
        if(withBody) {
            return Request.Get("https://api.github.com/user?access_token=" + accessToken)
                    .execute();
        } else {
            return Request.Head("https://api.github.com/user?access_token=" + accessToken)
                    .execute();
        }
    }
}
