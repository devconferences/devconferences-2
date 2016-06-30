package org.devconferences.github;

import com.google.gson.annotations.SerializedName;

public class GitHubAuthenticationResponse {
    @SerializedName("access_token")
    public String accessToken;
    @SerializedName("token_type")
    public String tokenType;
    public String scope;
}
