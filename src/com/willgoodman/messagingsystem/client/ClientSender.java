package com.willgoodman.messagingsystem.client;

import com.willgoodman.messagingsystem.Report;

import java.io.*;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.Cipher;


// Repeatedly reads text from the user and sends them to the server

public class ClientSender extends Thread {

  private PrintStream server;
  private final String HOSTNAME;
  private static final String FILENAME = "publicKeys.txt";
  private PublicKey serverPublicKey = null;
  private static final String ENCRYPTION_ALGORITHM = "RSA";
  private static final String QUIT_MESSAGE = "quit";
  private static final String REGISTER = "register";
  private static final String LOGIN = "login";
  private static final String LOGOUT = "logout";
  private static final String PREVIOUS = "previous";
  private static final String NEXT = "next";
  private static final String DELETE = "delete";
  private static final String SEND = "send";
  private static final int HALF_SECOND = 500;
  //Error messages:
  private static final String NOT_RECOGNISED = "Command not recognised";
  private static final String DISCONNECTED = "Disconnected";
  private static final String THREAD_CANT_SLEEP = "Thread could not sleep";
  private static final String CONNECTION_BROKE = "Communication broke in ClientSender";
  private static final String INVALID_KEY = "Invalid key spec";
  private static final String ALGORITHM_DOESNT_EXIST = "Encryption algorithm doesn't exist";
  private static final String FILE_READ_ERROR = "Error reading the file";
  private static final String ENCRYPT_ERROR = "Couldn't encrypt";

  public ClientSender(PrintStream server, String hostname) {
    this.server = server;
    this.HOSTNAME = hostname;
  }

  public void run() {

    //Must get the server's public key from the text file
    serverPublicKey = recoverKey();   
    
    // So that we can use the method readLine:
    BufferedReader user = new BufferedReader(new InputStreamReader(System.in));

    try {
      // Then loop forever sending messages to recipients via the server:
      while (true) {
        String commandPlainText = user.readLine();
        String command = "";
        try {
          command = encrypt(commandPlainText);
        } catch (Exception e) {
          Report.errorAndGiveUp(ENCRYPT_ERROR);
        } 
        
        
        if (commandPlainText.equals(QUIT_MESSAGE)) {
            server.println(command);
            //ClientReceiver must close before ClientSender, otherwise we would get an I/O Exception
            try {
                Thread.sleep(HALF_SECOND);
            } catch (InterruptedException e) {
                Report.error(THREAD_CANT_SLEEP);
            }
            break;
          
        } else {
            //Depending on which command is used, we may have to get further input before contacting the server
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

  private PublicKey recoverKey() {
    PublicKey publicKey = null;
    try {
      BufferedReader reader = new BufferedReader(new FileReader(FILENAME));
      String currentLine = "";
      while ((currentLine = reader.readLine()) != null) {
        if(currentLine.contains(HOSTNAME)) {
          break;
        }
      }
      String encodedKey = currentLine.split("-")[1];
      byte[] decodedKey = Base64.getDecoder().decode(encodedKey);

      try {
        KeyFactory keyFactory = KeyFactory.getInstance(ENCRYPTION_ALGORITHM);
        try {
        publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
        } catch (InvalidKeySpecException e) {
          Report.errorAndGiveUp(INVALID_KEY);
        }
      } catch (NoSuchAlgorithmException e) {
        Report.errorAndGiveUp(ALGORITHM_DOESNT_EXIST);
      }     
      
    } catch (IOException ex) {
      Report.errorAndGiveUp(FILE_READ_ERROR);
    }
    return publicKey;        
  }


  private String encrypt(String plainText) throws Exception {
    Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
    byte[] byteEncrypted = cipher.doFinal(plainText.getBytes());
    String stringEncrypted = Base64.getEncoder().encodeToString(byteEncrypted);
    return stringEncrypted;
    
  }
}

