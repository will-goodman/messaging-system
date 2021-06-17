package com.willgoodman.messagingsystem.client;

import com.willgoodman.messagingsystem.Commands;
import com.willgoodman.messagingsystem.Config;
import com.willgoodman.messagingsystem.Report;

import java.io.*;
import java.security.PrivateKey;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

// Gets messages from other clients via the server (by the
// ServerSender thread).

public class ClientReceiver extends Thread {

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
      while (!serverResponse.equals(Commands.QUIT)) {
        serverResponse = decrypt(fromServer.readLine());
        System.out.println(serverResponse);
      }
      System.out.println("end");
    } catch (IOException ex) {
      Report.errorAndGiveUp("Error reading from server: " + ex.getMessage());
    } catch (IllegalBlockSizeException | BadPaddingException ex) {
      Report.errorAndGiveUp("Error decrypting response from server: " + ex.getMessage());
    }
  }

  private String decrypt(String cipherText) throws IllegalBlockSizeException, BadPaddingException {
    return new String (this.decryptCipher.doFinal(Base64.getDecoder().decode(cipherText)));
  }

}


