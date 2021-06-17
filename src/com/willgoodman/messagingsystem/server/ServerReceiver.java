package com.willgoodman.messagingsystem.server;

import com.willgoodman.messagingsystem.*;

import java.io.*;
import java.util.Hashtable;
import java.util.ArrayList;
import java.security.PrivateKey;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

// Gets messages from client and puts them in a queue, for another
// thread to forward to the appropriate client.

public class ServerReceiver extends Thread {
  private final String clientName;
  private final BufferedReader fromClient;
  private Hashtable<String,User> users;
  private ArrayList<String> connectedClients;
  private Cipher DECRYPT_CIPHER;

  public ServerReceiver(String clientName, BufferedReader fromClient, PrivateKey privateKey,
                        Hashtable<String,User> users, ArrayList<String> connectedClients) {
    this.clientName = clientName;
    this.fromClient = fromClient;
    this.users = users;
    this.connectedClients = connectedClients;

    try {
      this.DECRYPT_CIPHER = Cipher.getInstance(Config.ENCRYPTION_ALGORITHM);
      this.DECRYPT_CIPHER.init(Cipher.DECRYPT_MODE, privateKey);
    } catch (Exception ex) {
      Report.errorAndGiveUp("Error initialising decryption cipher: " + ex.getMessage());
    }
  }

  public void run() {
    String command = "";

    while (!command.equals(Commands.QUIT)) {
      try {
        command = decrypt(fromClient.readLine());
        System.out.println("Command from " + this.clientName + ": " + command);

        switch (command) {
          case Commands.REGISTER:

            break;
          default:
            break;
        }
      } catch (IOException ex) {
        Report.error("Error reading from client: " + ex.getMessage());
        break;
      } catch (IllegalBlockSizeException | BadPaddingException ex) {
        Report.error("Error decrypting message: " + ex.getMessage());
        break;
      }
    }

    System.out.println(clientName + " quit.");
    this.connectedClients.remove(this.clientName);
  }

  private String decrypt(String cipherText) throws IllegalBlockSizeException, BadPaddingException {
    return new String (DECRYPT_CIPHER.doFinal(Base64.getDecoder().decode(cipherText)));
  }

}

