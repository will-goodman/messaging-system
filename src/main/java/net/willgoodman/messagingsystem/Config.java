package com.willgoodman.messagingsystem;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

/**
 * Stores common settings/functions required by both the Server and Client.
 * Saves having to make changes in multiple classes.
 *
 * @author Will Goodman
 * */
public class Config {

    public static final int PORT = 4444;
    public static final String ENCRYPTION_ALGORITHM = "RSA";
    public static final int KEY_SIZE = 2048;

    /**
     * Generates a private and public key pair for either the server or a client.
     *
     * @return a public key and its corresponding private key
     * @throws NoSuchAlgorithmException if the chosen encryption algorithm is invalid
     */
    public static KeyPair generateKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ENCRYPTION_ALGORITHM);
        keyPairGenerator.initialize(KEY_SIZE);

        return keyPairGenerator.genKeyPair();
    }

}
