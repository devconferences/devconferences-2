package org.devconferences.security;


import org.junit.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

/**
 * Created by chris on 05/06/15.
 */
public class SecureCookieTest {
    Encrypter secureCookie = new Encrypter();

    @Test
    public void testEncryptDecrypt(){
        String testString = "testEncryptDecrypt";
        String encrypted = secureCookie.encrypt(testString);

        assertFalse(testString.equals(encrypted));
        assertEquals(testString, secureCookie.decrypt(encrypted));
    }
}
