package com.willgoodman.messagingsystem.client;

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
  private static final String ENCRYPTION_ALGORITHM = "RSA";
  private static final String QUIT_MESSAGE = "quit";
  //Error messages
  private static final String SERVER_DIED = "Server seems to have died ";
  private static final String DECRYPT_ERROR = "Couldn't decrypt";

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
      while (!serverResponse.equals(QUIT_MESSAGE)) {
        serverResponse = decrypt(fromServer.readLine());
        System.out.println(serverResponse);
      }
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


