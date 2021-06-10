package com.willgoodman.messagingsystem;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.Cipher;

// Continuously reads from message queue for a particular client,
// forwarding to the client.

public class ServerSender extends Thread {
  private BlockingQueue<Message> clientQueue;
  private PrintStream client;
  private MessageList messageList;
  private int currentMessage;
  private PublicKey clientPublicKey = null;
  private final String THREAD_NAME;
  private static final String FILENAME = "publicKeys.txt";
  private static final String QUIT_MESSAGE = "quit";
  private static final int HALF_SECOND = 500;
  private static final String ENCRYPTION_ALGORITHM = "RSA";
  private static final int NEW_MESSAGE_CLIENTNAME_INDEX = 14;
  private static final int PREVIOUS_CLIENTNAME_INDEX = 11;
  private static final int NEXT_CLIENTNAME_INDEX = 7;
  private static final int DELETE_CLIENTNAME_INDEX = 9;
  private static final int LOGIN_CLIENTNAME_INDEX = 8;
  private static final String NEW_MESSAGE = "new message - ";
  private static final String PREVIOUS = "previous - ";
  private static final String NEXT = "next - ";
  private static final String DELETE = "delete - ";
  private static final String LOGIN = "login - ";
  private static final int EMPTY_MESSAGES = -1;
  private static final int FIRST_MESSAGE = 0;
  //Output messages:
  private static final String NO_MESSAGES = "No Messages";
  private static final String NO_PREVIOUS = "No previous messages";
  private static final String NO_NEXT = "No succeeding messages";
  private static final String NO_MORE_MESSAGES = "No more messages";
  private static final String LOGGED_IN = "Logged in";
  private static final String INVALID_KEY = "Invalid key spec";
  private static final String ALGORITHM_DOESNT_EXIST = "Encryption algorithm doesn't exist";
  private static final String FILE_READ_ERROR = "Error reading the file";
  private static final String ENCRYPT_ERROR = "Couldn't encrypt";
  

  public ServerSender(BlockingQueue<Message> q, PrintStream c, MessageList m, String threadName) {
    clientQueue = q;   
    client = c;
    messageList = m;
    THREAD_NAME = threadName;
  }

  public void run() {
    try {
      Thread.sleep(HALF_SECOND);
    } catch (InterruptedException ex) {

    }
  
    clientPublicKey = recoverKey();
    currentMessage = EMPTY_MESSAGES;
    while (true) {

      

      try {
        Message msg = clientQueue.take(); 
        String msgText = msg.getText();
        String clientsName;
        ArrayList<Message> clientsList;
        int clientsListSize;
        
       
        if (msgText.equals(QUIT_MESSAGE)) {
            try {
              client.println(encrypt(QUIT_MESSAGE));
            } catch (Exception ex) {
              Report.errorAndGiveUp(ENCRYPT_ERROR);
            }
            break;
        } else if (msgText.contains(NEW_MESSAGE)) {
          clientsName = msgText.substring(NEW_MESSAGE_CLIENTNAME_INDEX);
          clientsList = messageList.get(clientsName);
          clientsListSize = clientsList.size();
          Message latestMessage = clientsList.get(clientsListSize - 1);
          try {
            String latestSender = latestMessage.getSender();
            String latestText = latestMessage.getText();
            //Must only let a thread quit if the message "quit" was sent by itself to itself. If the message "quit" is sent by another thread, then it must be sent as a regular message
            if (clientsName.equals(latestSender) && latestText.equals(QUIT_MESSAGE)) {
              client.println(encrypt(QUIT_MESSAGE));
            } else {
              client.println(encrypt(latestMessage.toString()));
            }
          } catch (Exception ex) {
            Report.errorAndGiveUp(ENCRYPT_ERROR);
          }
          currentMessage = clientsListSize - 1;
        } else if (msgText.contains(PREVIOUS)) {
          clientsName = msgText.substring(PREVIOUS_CLIENTNAME_INDEX);
          clientsList = messageList.get(clientsName);
          try {
            if (currentMessage == EMPTY_MESSAGES) {
              client.println(encrypt(NO_MESSAGES));
            } else if (currentMessage > FIRST_MESSAGE) {
            
              Message previousMessage = clientsList.get(--currentMessage);
              client.println(encrypt(previousMessage.toString()));
            } else {
            
              client.println(encrypt(NO_PREVIOUS));
              Message currentMessageText = clientsList.get(currentMessage);
              client.println(encrypt(currentMessageText.toString()));
            }
          } catch (Exception ex) {
            Report.errorAndGiveUp(ENCRYPT_ERROR);
          }
        } else if (msgText.contains(NEXT)) {
          clientsName = msgText.substring(NEXT_CLIENTNAME_INDEX);
          clientsList = messageList.get(clientsName);
          clientsListSize = clientsList.size();
          try {
            if (currentMessage == EMPTY_MESSAGES) {
              client.println(encrypt(NO_MESSAGES));
            } else if (currentMessage < clientsListSize - 1) {
              Message nextMessage = clientsList.get(++currentMessage);
             
              client.println(encrypt(nextMessage.toString()));
            } else {
              client.println(encrypt(NO_NEXT));
              Message currentMessageText = clientsList.get(currentMessage);
              client.println(encrypt(currentMessageText.toString()));
            }
          } catch (Exception ex) {
            Report.errorAndGiveUp(ENCRYPT_ERROR);
          }
        } else if (msgText.contains(DELETE)) {
          clientsName = msgText.substring(DELETE_CLIENTNAME_INDEX);
          clientsList = messageList.get(clientsName);
          clientsListSize = clientsList.size();
          try {
            if (currentMessage == EMPTY_MESSAGES) {
              client.println(encrypt(NO_MESSAGES));
            } else if (currentMessage == FIRST_MESSAGE) { 
              client.println(encrypt(NO_MORE_MESSAGES)); 
              clientsList.remove(currentMessage--);        
                     
            } else if (currentMessage < clientsListSize - 1) {
              Message nextMessage = clientsList.get(currentMessage + 1);  
              client.println(encrypt(nextMessage.toString()));
              clientsList.remove(currentMessage);
             

            } else {
              clientsList.remove(currentMessage);
            
              Message previousMessage = clientsList.get(--currentMessage);
              client.println(encrypt(previousMessage.toString()));            
            }
          } catch (Exception ex) {
            Report.errorAndGiveUp(ENCRYPT_ERROR);
          }
          
        } else if (msgText.contains(LOGIN)) {
          try {
            client.println(encrypt(LOGGED_IN));
            clientsName = msgText.substring(LOGIN_CLIENTNAME_INDEX);
            clientsList = messageList.get(clientsName);
            clientsListSize = clientsList.size();
            if (clientsListSize > FIRST_MESSAGE) {
              Message latestMessage = clientsList.get(clientsListSize - 1);
              client.println(encrypt(latestMessage.toString()));
            } else {
              client.println(encrypt(NO_MESSAGES));
            }
            currentMessage = clientsListSize - 1;
          } catch (Exception ex) {
          Report.errorAndGiveUp(ENCRYPT_ERROR);
        }
        } else {
          try {
            client.println(encrypt(msgText));
          } catch (Exception ex) {
            Report.errorAndGiveUp(ENCRYPT_ERROR);
          }
        }
        
        
      }
      catch (InterruptedException e) {
        // Do nothing and go back to the infinite while loop.
      }
    }
  }

  private PublicKey recoverKey() {
    PublicKey publicKey = null;
    try {
      BufferedReader reader = new BufferedReader(new FileReader(FILENAME));
      String currentLine = "";
      while ((currentLine = reader.readLine()) != null) {
        if(currentLine.contains(THREAD_NAME)) {
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
    cipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);
    byte[] byteEncrypted = cipher.doFinal(plainText.getBytes());
    String stringEncrypted = Base64.getEncoder().encodeToString(byteEncrypted);
    return stringEncrypted;
    
  }
}


