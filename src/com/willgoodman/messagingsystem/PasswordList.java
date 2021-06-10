package com.willgoodman.messagingsystem;

import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Arrays;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
public class PasswordList {
  private HashMap<String, byte[]> passwords = new HashMap<String, byte[]>();
  private HashMap<String, byte[]> salts = new HashMap<String, byte[]>();
  private final Random RANDOM = new SecureRandom();  
  private static final int ITERATIONS = 10000;
  private static final int KEY_LENGTH = 256;
  private static final int SALT_BIT_LENGTH = 32;
  private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA1";
  //Error messages:
  private static final String NO_STORED_PASSWORD = "User does not have any stored passwords";
  private static final String HASH_ERROR = "Couldn't hash password";

  public void add(String username, String password) {
    byte[] salt = generateSalt();
    byte[] hashedPassword = hashPassword(password, salt);
    passwords.put(username, hashedPassword);
    salts.put(username, salt);
  }

  /*Code for the salt generation, hashing and authentication taken from:
  https://stackoverflow.com/questions/18142745/how-do-i-generate-a-salt-in-java-for-salted-hash
 */
  public boolean authenticate(String username, String password) {
    if (passwords.containsKey(username)) {
      byte[] salt = salts.get(username);
      
      byte[] hashedNewPassword = hashPassword(password, salt);
      char[] passwordChars = password.toCharArray();
      Arrays.fill(passwordChars, Character.MIN_VALUE);
      
      byte[] hashedStoredPassword = passwords.get(username);
 
      if (hashedNewPassword.length != hashedStoredPassword.length) {
        return false;
      }
      for (int i = 0; i < hashedNewPassword.length; i++) {
        if (hashedNewPassword[i] != hashedStoredPassword[i]) {
          return false;
        }
      }

      return true;
    } else {
      Report.errorAndGiveUp(NO_STORED_PASSWORD);
      return false;
    }
  }

  
  private byte[] generateSalt() {
    byte[] salt = new byte[SALT_BIT_LENGTH];
    RANDOM.nextBytes(salt);
    return salt;
  }

  
  private byte[] hashPassword(String plainText, byte[] salt) {
    char[] plainTextChars = plainText.toCharArray();
    PBEKeySpec spec = new PBEKeySpec(plainTextChars, salt, ITERATIONS, KEY_LENGTH);
    Arrays.fill(plainTextChars, Character.MIN_VALUE);

    try {
      SecretKeyFactory secretKeyFact = SecretKeyFactory.getInstance(HASH_ALGORITHM);
      return secretKeyFact.generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      Report.errorAndGiveUp(HASH_ERROR);
      return null;
    } finally {
      spec.clearPassword();
    }
  }
}
