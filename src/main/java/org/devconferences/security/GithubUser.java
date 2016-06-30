package org.devconferences.security;

import com.google.gson.annotations.SerializedName;

class GithubUser {
    public String login;
    public long id;
    @SerializedName("avatar_url")
    public String avatarUrl;
    public String email;

}
