package org.devconferences.security;

import junit.framework.Assert;
import org.devconferences.users.User;
import org.devconferences.users.UsersRepository;
import org.junit.Test;

import java.util.HashMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Created by chris on 10/06/15.
 */
public class AuthenticationTest {
    Authentication authentication = new Authentication(new Encrypter(), new UsersRepository());

    @Test
    public void testExtractUserFromResponse_emptyMap() {
        User user = authentication.extractUserFromResponse(new HashMap<String, Object>());

        assertNull(user.avatarURL);
        assertNull(user.email);
        assertNull(user.login);
        assertNull(user.id);

    }

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
