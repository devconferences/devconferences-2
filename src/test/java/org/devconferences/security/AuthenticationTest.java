package org.devconferences.security;

import org.devconferences.users.User;
import org.devconferences.users.UsersRepository;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by chris on 10/06/15.
 */
public class AuthenticationTest {
    Authentication authentication = new Authentication(new Encrypter(), new UsersRepository());

    @Test
    public void test() {
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
}
