package org.devconferences.security;

import org.devconferences.env.EnvUtils;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.keys.AesKey;
import org.jose4j.lang.JoseException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * Created by chris on 05/06/15.
 */
public class Encrypter {
    public static final String ENCRYPTION_KEY = "ENCRYPTION_KEY";
    public static final String AES = "AES";
    private byte[] key;


    public Encrypter() {
        String base64Key = EnvUtils.fromEnv(ENCRYPTION_KEY, "HOcxU5XN+ymJHCr6NmaHhw==");
        key = Base64.getDecoder().decode(base64Key.getBytes());
    }


    public String encrypt(String toEncrypt) {
        JsonWebEncryption jsonWebEncryption = new JsonWebEncryption();
        jsonWebEncryption.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.A128KW);
        jsonWebEncryption.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
        jsonWebEncryption.setPayload(toEncrypt);
        jsonWebEncryption.setKey(new AesKey(key));
        try {
            return jsonWebEncryption.getCompactSerialization();
        } catch (JoseException e) {
            throw new RuntimeException(e);
        }
//
//        try {
//            Cipher cipher = Cipher.getInstance(AES);
//            SecretKeySpec secretKeySpec =
//                    new SecretKeySpec(key, AES);
//            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
//            byte[] encrypted = cipher.doFinal(toEncrypt.getBytes());
//            return new String(Base64.getEncoder().encode(encrypted));
//        } catch (GeneralSecurityException e) {
//            throw new RuntimeException(e);
//        }
    }

    public String decrypt(String toDecrypt) {
        JsonWebEncryption jsonWebEncryption = new JsonWebEncryption();

        jsonWebEncryption.setKey(new AesKey(key));
        try {
            jsonWebEncryption.setCompactSerialization(toDecrypt);
            return jsonWebEncryption.getPayload();
        } catch (JoseException e) {
            throw new RuntimeException(e);
        }


//        try {
//
//            Cipher cypher = Cipher.getInstance(AES);
//            SecretKeySpec secretKeySpec =
//                    new SecretKeySpec(key, AES);
//            cypher.init(Cipher.DECRYPT_MODE, secretKeySpec);
//            byte[] decoded = Base64.getDecoder().decode(toDecrypt.getBytes());
//            return new String(cypher.doFinal(decoded));
//        } catch (GeneralSecurityException e) {
//            throw new RuntimeException(e);
//        }
    }
}
