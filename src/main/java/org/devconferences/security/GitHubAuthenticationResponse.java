package org.devconferences.security;

import com.google.gson.annotations.SerializedName;

/**
 * Created by ronan on 20/06/16.
 */
public class GitHubAuthenticationResponse {
    @SerializedName("access_token")
    public String accessToken;
    @SerializedName("token_type")
    public String tokenType;
    public String scope;
}
