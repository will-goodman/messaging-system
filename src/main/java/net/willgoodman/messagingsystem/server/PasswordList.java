package net.willgoodman.messagingsystem.server;

import net.willgoodman.messagingsystem.Report;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
public class PasswordList {
  private HashMap<String, byte[]> passwords = new HashMap<String, byte[]>();
  private HashMap<String, byte[]> salts = new HashMap<String, byte[]>();
  private final Random RANDOM = new SecureRandom();
  private static final int ITERATIONS = 10000;
  private static final int KEY_LENGTH = 256;

  public void add(String username, String password) {
    byte[] salt = generateSalt();
    byte[] hashedPassword = hashPassword(password, salt);
    passwords.put(username, hashedPassword);
    salts.put(username, salt);
  }

  public void remove(String username) {

  }

  public boolean authenticate(String username, String password) {
    if (passwords.containsKey(username)) {
      byte[] salt = salts.get(username);
      byte[] hashedNewPassword = hashPassword(password, salt);
      byte[] hashedStoredPassword = passwords.get(username);
      if (hashedNewPassword == hashedStoredPassword) {
        return true;
      } else {
        return false;
      }
    } else {
      Report.errorAndGiveUp("User does not have passwords stored in the hashmap");
    }
      return false;
  }

  /*Code for the salt generation and hashing taken from:
  https://stackoverflow.com/questions/18142745/how-do-i-generate-a-salt-in-java-for-salted-hash
 */
  private byte[] generateSalt() {
    byte[] salt = new byte[32];
    RANDOM.nextBytes(salt);
    return salt;
  }

  
  private byte[] hashPassword(String plainText, byte[] salt) {
    char[] plainTextChars = plainText.toCharArray();
    PBEKeySpec spec = new PBEKeySpec(plainTextChars, salt, ITERATIONS, KEY_LENGTH);
    Arrays.fill(plainTextChars, Character.MIN_VALUE);

    try {
      SecretKeyFactory secretKeyFact = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      return secretKeyFact.generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      Report.errorAndGiveUp("Couldn't hash password");
    } finally {
      spec.clearPassword();
    }
      return salt;
  }
}
