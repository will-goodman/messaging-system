package com.willgoodman.messagingsystem.server;

import com.willgoodman.messagingsystem.Config;
import com.willgoodman.messagingsystem.Report;

import java.io.*;
import java.security.InvalidKeyException;
import java.util.Hashtable;
import java.util.ArrayList;
import java.security.PublicKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Queue;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

// Continuously reads from message queue for a particular client,
// forwarding to the client.

public class ServerSender extends Thread {
  private String clientName;
  private PrintStream toClient;
  private Hashtable<String, User> users;
  private Hashtable<String, Queue<Message>> clients;
  private Cipher encryptCipher;

  public ServerSender(String clientName, PrintStream toClient, PublicKey clientPublicKey, Hashtable<String, User> users,
                      Hashtable<String, Queue<Message>> clients) {
    this.clientName = clientName;
    this.toClient = toClient;
    this.users = users;
    this.clients = clients;

    try {
      this.encryptCipher = Cipher.getInstance(Config.ENCRYPTION_ALGORITHM);
      this.encryptCipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
      Report.errorAndGiveUp("Error initialising encryption cipher: " + ex.getMessage());
    } catch (InvalidKeyException ex) {
      Report.errorAndGiveUp("Invalid client public key");
    }
  }

  public void run() {
    while (this.clients.containsKey(this.clientName)) {
      try {
        if (this.clients.get(this.clientName).size() > 0) {
          this.toClient.println(encrypt(this.clients.get(this.clientName).remove().toString()));
        }
      } catch (IllegalBlockSizeException | BadPaddingException ex) {
        Report.errorAndGiveUp("Error encrypting message: " + ex.getMessage());
      }
    }

  }

  private String encrypt(String plainText) throws IllegalBlockSizeException, BadPaddingException {
    return Base64.getEncoder().encodeToString(encryptCipher.doFinal(plainText.getBytes()));
  }
}


