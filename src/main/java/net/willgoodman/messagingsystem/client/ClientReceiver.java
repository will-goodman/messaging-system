package net.willgoodman.messagingsystem.client;

import net.willgoodman.messagingsystem.Report;

import java.io.*;
import java.security.PrivateKey;
import java.util.Base64;
import javax.crypto.Cipher;

// Gets messages from other clients via the server (by the
// ServerSender thread).

public class ClientReceiver extends Thread {

  private BufferedReader server;
  private final PrivateKey PRIVATE_KEY;
  private static final String QUIT_MESSAGE = "quit";
  private static final String SERVER_DIED = "Server seems to have died ";

  ClientReceiver(BufferedReader server, PrivateKey privateKey) {
    this.server = server;
    this.PRIVATE_KEY = privateKey;
  }

  public void run() {
    // Print to the user whatever we get from the server:
    try {
      while (true) {
        String s = "";
        try {
          s = decrypt(server.readLine()); // Matches FFFFF in ServerSender.java
        } catch (Exception ex) {
          Report.errorAndGiveUp("Couldn't decrypt");
        }
        //ClientReceiver must close before ClientSender, otherwise we would get an I/O Exception
        Thread.yield();
        
        if (s != null) {
            if (s.equals(QUIT_MESSAGE)) {                            
                break;
            } else {
                System.out.println(s);
            }
        } else {
          Report.errorAndGiveUp(SERVER_DIED);
        } 
      }
    }
    catch (Exception e) {
      Report.errorAndGiveUp(SERVER_DIED + e.getMessage());
    }
  }

  public String decrypt(String encrypted) throws Exception {
    byte [] byteEncrypted = Base64.getDecoder().decode(encrypted);
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.DECRYPT_MODE, PRIVATE_KEY);
    byte[] bytePlainText = cipher.doFinal(byteEncrypted);
    return new String (bytePlainText);
  }
}

/*

 * The method readLine returns null at the end of the stream

 * It may throw IoException if an I/O error occurs

 * See https://docs.oracle.com/javase/8/docs/api/java/io/BufferedReader.html#readLine--


 */
