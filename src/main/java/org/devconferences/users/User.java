package org.devconferences.users;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chris on 05/06/15.
 */
public class User implements net.codestory.http.security.User {
    public static final String ADMIN = "ADMIN";
    public static final String EVENT_MANAGER = "EVENT_MANAGER";

    public String id;
    public String login;
    public String email;
    public String avatarURL;
    public final List<String> roles = new ArrayList<>();
    public final List<String> events = new ArrayList<>();
    public final Favourites favourites = new Favourites();
    public final List<Message> messages = new ArrayList<>();

    public class Favourites {
        public final List<String> cities = new ArrayList<>();
        public final List<String> tags = new ArrayList<>();
        public final List<String> conferences = new ArrayList<>();
        public final List<String> communities = new ArrayList<>();
        public final List<String> upcomingEvents = new ArrayList<>();
    }

    public class Message {
        public String id;
        public Long date;
        public String text;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Message message = (Message) o;

            if (!id.equals(message.id)) return false;
            if (!date.equals(message.date)) return false;
            return text.equals(message.text);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + date.hashCode();
            result = 31 * result + text.hashCode();
            return result;
        }
    }

    public User(String login, String id, String email, String avatarURL) {
        this.login = login;
        this.id = id;
        this.avatarURL = avatarURL;
        this.email = email;
    }

    @Override
    public String name() {
        return login;
    }

    @Override
    public String login() {
        return login;
    }

    @Override
    public String[] roles() {
        return roles.toArray(new String[roles.size()]);
    }

    @Override
    public boolean isInRole(String expectedRole) {
        return roles.contains(expectedRole);
    }
}
