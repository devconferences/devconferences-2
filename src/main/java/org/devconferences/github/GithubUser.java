package org.devconferences.github;

import com.google.gson.annotations.SerializedName;

public class GithubUser {
    public String login;
    public long id;
    @SerializedName("avatar_url")
    public String avatarUrl;
    public String email;

}
