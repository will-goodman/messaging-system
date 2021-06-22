package com.willgoodman.messagingsystem.client;

import com.willgoodman.messagingsystem.Config;
import com.willgoodman.messagingsystem.Report;

import java.io.*;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 * Receives all messages from the server (both sent by the server and other clients via the ServerSender thread)
 */
public class ClientReceiver extends Thread {

  private BufferedReader fromServer;
  private Cipher decryptCipher;
  private AtomicBoolean disconnect;

  public ClientReceiver(BufferedReader fromServer, PrivateKey privateKey, AtomicBoolean disconnect) {
    this.fromServer = fromServer;
    this.disconnect = disconnect;

    try {
      this.decryptCipher = Cipher.getInstance(Config.ENCRYPTION_ALGORITHM);
      this.decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
    } catch (Exception ex) {
      Report.errorAndGiveUp("Error initialising decryption cipher: " + ex.getMessage());
    }
  }

  public void run() {
    try {
      while (!this.disconnect.get()) {
        // .ready() check ensures the value of disconnect is checked regularly, rather than waiting forever for a
        // response from the server
        if (this.fromServer.ready()) {
          System.out.println(decrypt(fromServer.readLine()));
        }
      }
    } catch (IOException ex) {
      Report.errorAndGiveUp("Error reading from server: " + ex.getMessage());
    } catch (IllegalBlockSizeException | BadPaddingException ex) {
      Report.errorAndGiveUp("Error decrypting response from server: " + ex.getMessage());
    }

    try {
      this.fromServer.close();
      System.out.println("Connection closed.");
    } catch (IOException ex) {
      System.out.println("Error closing connection: " + ex.getMessage());
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


