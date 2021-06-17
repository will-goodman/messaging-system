package com.willgoodman.messagingsystem.server;

import com.willgoodman.messagingsystem.*;

import java.io.*;
import java.util.Hashtable;
import java.util.ArrayList;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Queue;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

// Gets messages from client and puts them in a queue, for another
// thread to forward to the appropriate client.

public class ServerReceiver extends Thread {
  private final String clientName;
  private final BufferedReader fromClient;
  private Hashtable<String,User> users;
  private Hashtable<String,Queue<Message>> clients;
  private Hashtable<String, String> loggedInUsers;
  private Cipher DECRYPT_CIPHER;

  public ServerReceiver(String clientName, BufferedReader fromClient, PrivateKey privateKey,
                        Hashtable<String,User> users, Hashtable<String,Queue<Message>> clients,
                        Hashtable<String, String> loggedInUsers) {
    this.clientName = clientName;
    this.fromClient = fromClient;
    this.users = users;
    this.clients = clients;
    this.loggedInUsers = loggedInUsers;

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
        command = decrypt(this.fromClient.readLine());
        System.out.println("Command from " + this.clientName + ": " + command);

        switch (command) {
          case Commands.REGISTER:
            String username = decrypt(this.fromClient.readLine());
            if (!this.users.containsKey(username)) {
              this.users.put(username, new User(username));
              System.out.println("User " + username + " created.");

              this.clients.get(this.clientName).offer(new Message(this.clientName, "User " + username + " created."));
            } else {
              System.out.println("User" + username + " already exists.");
              this.clients.get(this.clientName).offer(new Message(this.clientName, "User " + username + " already exists."));
            }
            break;
          case Commands.LOGIN:
            username = decrypt(this.fromClient.readLine());
            if (this.users.containsKey(username)) {
              this.loggedInUsers.put(this.clientName, username);
              System.out.println("User " + username + " logged in.");
              this.clients.get(this.clientName).offer(new Message(this.clientName, "User " + username + " logged in."));
            } else {
              System.out.println("User " + username + " doesn't exist.");
              this.clients.get(this.clientName).offer(new Message(this.clientName, "User " + username + " doesn't exist."));
            }
            break;
          case Commands.LOGOUT:
            if (!this.loggedInUsers.containsKey(this.clientName)) {
              System.out.println("No user currently logged in.");
              this.clients.get(this.clientName).offer(new Message(this.clientName, "No user currently logged in."));
            } else {
              username = this.loggedInUsers.get(this.clientName);
              this.loggedInUsers.remove(this.clientName);
              System.out.println("User " + username + " logged out.");
              this.clients.get(this.clientName).offer(new Message(this.clientName, "User " + username + " logged out."));
            }
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
    this.clients.remove(this.clientName);
  }

  private String decrypt(String cipherText) throws IllegalBlockSizeException, BadPaddingException {
    return new String (DECRYPT_CIPHER.doFinal(Base64.getDecoder().decode(cipherText)));
  }

}

