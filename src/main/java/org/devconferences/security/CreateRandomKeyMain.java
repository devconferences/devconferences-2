package org.devconferences.security;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Created by chris on 05/06/15.
 */
public class CreateRandomKeyMain {
    public static void main(String[] args) {
        SecureRandom rnd = new SecureRandom();
        byte[] key = new byte[16];
        rnd.nextBytes(key);

        System.out.println(new String(Base64.getEncoder().encode(key)));
    }
}
