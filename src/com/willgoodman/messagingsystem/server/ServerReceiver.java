package com.willgoodman.messagingsystem.server;

import com.willgoodman.messagingsystem.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.security.PrivateKey;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

// Gets messages from client and puts them in a queue, for another
// thread to forward to the appropriate client.

public class ServerReceiver extends Thread {
  private final String clientName;
  private final BufferedReader fromClient;
  private ArrayList<User> users;
  private Cipher DECRYPT_CIPHER;
  private static final String QUIT = "quit";

  public ServerReceiver(String clientName, BufferedReader fromClient, PrivateKey privateKey, ArrayList<User> users) {
    this.clientName = clientName;
    this.fromClient = fromClient;
    this.users = users;

    try {
      this.DECRYPT_CIPHER = Cipher.getInstance(Config.ENCRYPTION_ALGORITHM);
      this.DECRYPT_CIPHER.init(Cipher.DECRYPT_MODE, privateKey);
    } catch (Exception ex) {
      Report.errorAndGiveUp("Error initialising decryption cipher: " + ex.getMessage());
    }
  }

  public void run() {
    String command = "";

    while (!command.equals(QUIT)) {
      try {
        command = decrypt(fromClient.readLine());
      } catch (IOException ex) {
        Report.errorAndGiveUp("Error reading from client: " + ex.getMessage());
      } catch (IllegalBlockSizeException | BadPaddingException ex) {
        Report.errorAndGiveUp("Error decrypting message: " + ex.getMessage());
      }
      System.out.println(command);
    }

    System.out.println(clientName + " quit.");
  }

  private String decrypt(String cipherText) throws IllegalBlockSizeException, BadPaddingException {
    return new String (DECRYPT_CIPHER.doFinal(Base64.getDecoder().decode(cipherText)));
  }

}

