package org.devconferences.security;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

/**
 * Created by chris on 05/06/15.
 */
public class EncrypterTest {
    Encrypter encrypter = new Encrypter();

    @Test
    public void testEncryptDecrypt(){
        String testString = "testEncryptDecrypt";
        String encrypted = encrypter.encrypt(testString);

        assertFalse(testString.equals(encrypted));
        assertEquals(testString, encrypter.decrypt(encrypted));
    }
}
