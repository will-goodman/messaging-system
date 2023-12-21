package net.willgoodman.messagingsystem.client;

import net.willgoodman.messagingsystem.Report;

import java.io.*;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.Cipher;


// Repeatedly reads recipient's nickname and text from the user in two
// separate lines, sending them to the server (read by ServerReceiver
// thread).

public class ClientSender extends Thread {

  private String nickname;
  private PrintStream server;
  private static final String FILENAME = "./src/main/resources/net/willgoodman/messagingsystem/publicKeys.txt";
  private PublicKey serverPublicKey = null;
  private static final String QUIT_MESSAGE = "quit";
  private static final String REGISTER = "register";
  private static final String LOGIN = "login";
  private static final String LOGOUT = "logout";
  private static final String PREVIOUS = "previous";
  private static final String NEXT = "next";
  private static final String DELETE = "delete";
  private static final String SEND = "send";
  private static final String NOT_RECOGNISED = "Command not recognised";
  private static final String DISCONNECTED = "Disconnected";
  private static final String THREAD_CANT_SLEEP = "Thread could not sleep";
  private static final String CONNECTION_BROKE = "Communication broke in ClientSender";

  ClientSender(PrintStream server) {
    //this.nickname = nickname;
    this.server = server;
  }

  public void run() {

    try {
      BufferedReader reader = new BufferedReader(new FileReader(FILENAME));
      String currentLine = "";
      while (!currentLine.contains("localhost")) {
        currentLine = reader.readLine();
      }
      String encodedKey = currentLine.split("-")[1];
      byte[] decodedKey = Base64.getDecoder().decode(encodedKey);

      try {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        try {
        serverPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
        } catch (InvalidKeySpecException e) {
          Report.errorAndGiveUp("Invalid key spec");
        }
      } catch (NoSuchAlgorithmException e) {
        Report.errorAndGiveUp("Encryption algorithm doesn't exist");
      }     
      
    } catch (IOException ex) {
      Report.errorAndGiveUp("Error reading the file");
    }    
    
    // So that we can use the method readLine:
    BufferedReader user = new BufferedReader(new InputStreamReader(System.in));

    try {
      // Then loop forever sending messages to recipients via the server:
      while (true) {
        String commandPlainText = user.readLine();
        String command = "";
        try {
          command = encrypt(commandPlainText);
          //System.out.println(new String (command));
        } catch (Exception e) {
          Report.errorAndGiveUp("Couldn't encrypt");
        } 
        
        
        if (commandPlainText.equals(QUIT_MESSAGE)) {
            server.println(command);
            //server.println("-1");
            //ClientReceiver must close before ClientSender, otherwise we would get an I/O Exception
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Report.error(THREAD_CANT_SLEEP);
            }
            break;
          
        } else {
            if (commandPlainText.equals(REGISTER) || commandPlainText.equals(LOGIN)) {
                String username = null;
                String password = null;                
                try {
                  username = encrypt(user.readLine());
                  password = encrypt(user.readLine());
                } catch (Exception e) {

                }
                server.println(command);
                server.println(username);
                server.println(password);
            } else if (commandPlainText.equals(LOGOUT) || commandPlainText.equals(PREVIOUS) || commandPlainText.equals(NEXT) || commandPlainText.equals(DELETE)) {
                server.println(command);
            } else if (commandPlainText.equals(SEND)) {
                String username = null;
                String message = null;                
                try {
                  username = encrypt(user.readLine());
                  message = encrypt(user.readLine());
                } catch (Exception e) {
          
                }
                server.println(command);
                server.println(username);
                server.println(message);
            } else {
                Report.error(NOT_RECOGNISED);
            }
            //server.println(recipient); // Matches CCCCC in ServerReceiver
            //server.println(text);      // Matches DDDDD in ServerReceiver
        }
      }
      //This will only execute if there were no errors along the way, otherwise the I/O Exception is triggered
      System.out.println(DISCONNECTED);
    }
    catch (IOException e) {
      Report.errorAndGiveUp(CONNECTION_BROKE 
                        + e.getMessage());
    }
  }

  public String encrypt(String plainText) throws Exception {
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
    byte[] byteEncrypted = cipher.doFinal(plainText.getBytes());
    String stringEncrypted = Base64.getEncoder().encodeToString(byteEncrypted);
    return stringEncrypted;
    
  }
}

/*

What happens if recipient is null? Then, according to the Java
documentation, println will send the string "null" (not the same as
null!). So maye we should check for that case! Paticularly in
extensions of this system.

 */
