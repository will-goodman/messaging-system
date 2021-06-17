package com.willgoodman.messagingsystem.server;

import com.willgoodman.messagingsystem.Config;
import com.willgoodman.messagingsystem.Report;

import java.io.*;
import java.security.InvalidKeyException;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

// Continuously reads from message queue for a particular client,
// forwarding to the client.

public class ServerSender extends Thread {
  private String clientName;
  private PrintStream toClient;
  private ArrayList<User> users;
  private ArrayList<String> connectedClients;
  private Cipher encryptCipher;

  public ServerSender(String clientName, PrintStream toClient, PublicKey clientPublicKey, ArrayList<User> users,
                      ArrayList<String> connectedClients) {
    this.clientName = clientName;
    this.toClient = toClient;
    this.users = users;
    this.connectedClients = connectedClients;

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
    while (this.connectedClients.contains(this.clientName)) {
      int x = 1;
    }
    System.out.println("ServerSender end");
  }

  private String encrypt(String plainText) throws IllegalBlockSizeException, BadPaddingException {
    return Base64.getEncoder().encodeToString(encryptCipher.doFinal(plainText.getBytes()));
  }
}


