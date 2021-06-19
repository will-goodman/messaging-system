package com.willgoodman.messagingsystem.client;

import com.willgoodman.messagingsystem.Config;
import com.willgoodman.messagingsystem.Report;

import java.io.*;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 * Receives all messages from the server (both sent by the server and other clients via the ServerSender thread)
 */
public class ClientReceiver extends Thread {

  private static final Pattern SERVER_QUIT_PATTERN = Pattern.compile("^From Server at \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}: quit$");
  private BufferedReader fromServer;
  private Cipher decryptCipher;

  public ClientReceiver(BufferedReader fromServer, PrivateKey privateKey) {
    this.fromServer = fromServer;

    try {
      this.decryptCipher = Cipher.getInstance(Config.ENCRYPTION_ALGORITHM);
      this.decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
    } catch (Exception ex) {
      Report.errorAndGiveUp("Error initialising decryption cipher: " + ex.getMessage());
    }
  }

  public void run() {
    try {
      String serverResponse = "";
      while (!SERVER_QUIT_PATTERN.matcher(serverResponse).find()) {
        serverResponse = decrypt(fromServer.readLine());
        System.out.println(serverResponse);
      }
      System.out.println("Connection closed.");
    } catch (IOException ex) {
      Report.errorAndGiveUp("Error reading from server: " + ex.getMessage());
    } catch (IllegalBlockSizeException | BadPaddingException ex) {
      Report.errorAndGiveUp("Error decrypting response from server: " + ex.getMessage());
    }
  }

  /**
   * Decrypts a message (String) received from the server.
   *
   * @param cipherText the original Base64 encoded cipher text
   * @return the decrypted String message
   * @throws IllegalBlockSizeException if the decryption fails
   * @throws BadPaddingException if the decryption fails
   */
  private String decrypt(String cipherText) throws IllegalBlockSizeException, BadPaddingException {
    return new String (this.decryptCipher.doFinal(Base64.getDecoder().decode(cipherText)));
  }

}


