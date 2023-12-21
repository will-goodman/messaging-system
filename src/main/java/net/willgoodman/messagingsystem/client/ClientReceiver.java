package com.willgoodman.messagingsystem.client;

import com.willgoodman.messagingsystem.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

  private static final Logger LOGGER = LogManager.getLogger(ClientReceiver.class);
  private BufferedReader fromServer;
  private Cipher decryptCipher;
  private AtomicBoolean disconnect;

  /**
   * Constructor
   *
   * @param fromServer receives response from the server
   * @param privateKey used to decrypt messages received from the server
   * @param disconnect boolean storing whether the client should disconnect from the server
   */
  public ClientReceiver(BufferedReader fromServer, PrivateKey privateKey, AtomicBoolean disconnect) {
    LOGGER.info("Constructing ClientReceiver");
    this.fromServer = fromServer;
    this.disconnect = disconnect;

    try {
      this.decryptCipher = Cipher.getInstance(Config.ENCRYPTION_ALGORITHM);
      this.decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
      LOGGER.debug("Created decryption cipher");
    } catch (Exception ex) {
      LOGGER.fatal(String.format("Error initialising decryption cipher (%s)", ex.getMessage()));
      System.exit(1);
    }
  }

  /**
   * Starts a new thread which prints any response received from the server
   */
  public void run() {
    LOGGER.info("Running ClientReceiver.run()");
    try {
      while (!this.disconnect.get()) {
        // .ready() check ensures the value of disconnect is checked regularly, rather than waiting forever for a
        // response from the server
        if (this.fromServer.ready()) {
          System.out.println(decrypt(fromServer.readLine()));
        }
      }
    } catch (IOException ex) {
      LOGGER.fatal(String.format("Error reading from server (%s)", ex.getMessage()));
    } catch (IllegalBlockSizeException | BadPaddingException ex) {
      LOGGER.fatal(String.format("Error decrypting response from server (%s)", ex.getMessage()));
    } finally {
      System.exit(1);
    }

    LOGGER.info("Stopped checking for new messages");

    try {
      this.fromServer.close();
      LOGGER.debug("Connection closed");
    } catch (IOException ex) {
      LOGGER.error(String.format("Error closing connection (%s)", ex.getMessage()));
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
    LOGGER.info("Running ClientReceiver.decrypt()");
    return new String (this.decryptCipher.doFinal(Base64.getDecoder().decode(cipherText)));
  }

}


