package net.willgoodman.messagingsystem.server;

import net.willgoodman.messagingsystem.Message;
import net.willgoodman.messagingsystem.Report;

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
  private int currentMessage = -1;
  private PublicKey clientPublicKey = null;
  private final String THREAD_NAME;
  private static final String FILENAME = "./src/main/resources/net/willgoodman/messagingsystem/publicKeys.txt";
  private static final String QUIT_MESSAGE = "quit";
  private static final String NO_MESSAGES = "No Messages";
  private static final String NO_PREVIOUS = "No previous messages";
  private static final String NO_NEXT = "No succeeding messages";
  private static final String NO_MORE_MESSAGES = "No more messages";
  private static final String LOGGED_IN = "Logged in";

  public ServerSender(BlockingQueue<Message> q, PrintStream c, MessageList m, String threadName) {
    clientQueue = q; 
    messageList = m;  
    client = c;
    THREAD_NAME = threadName;
  }

  public void run() {
    try {
      Thread.sleep(500);
    } catch (InterruptedException ex) {

    }
  
    try {
      BufferedReader reader = new BufferedReader(new FileReader(FILENAME));
      String currentLine = "";
      while ((currentLine = reader.readLine()) != null) {
        if(currentLine.contains(THREAD_NAME)) {
          //currentLine = reader.readLine();
          break;
        }
      }
      String encodedKey = currentLine.split("-")[1];
      byte[] decodedKey = Base64.getDecoder().decode(encodedKey);

      try {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        try {
        clientPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
        } catch (InvalidKeySpecException e) {
          Report.errorAndGiveUp("Invalid key spec");
        }
      } catch (NoSuchAlgorithmException e) {
        Report.errorAndGiveUp("Encryption algorithm doesn't exist");
      }     
      
    } catch (IOException ex) {
      Report.errorAndGiveUp("Error reading the file");
    }        
    while (true) {

      

      //client.println(threadName);
      try {
        Message msg = clientQueue.take(); // Matches EEEEE in ServerReceiver
        String msgText = msg.getText();
        String clientsName;
        ArrayList<Message> clientsList;
        int clientsListSize;
        
       
        if (msgText.equals(QUIT_MESSAGE)) {
            try {
              client.println(encrypt(QUIT_MESSAGE));
            } catch (Exception ex) {
              Report.errorAndGiveUp("Couldn't encrypt");
            }
            break;
        } else if (msgText.contains("new message - ")) {
          clientsName = msgText.substring(14);
          clientsList = messageList.get(clientsName);
          clientsListSize = clientsList.size();
          Message latestMessage = clientsList.get(clientsListSize - 1);
          try {
            if (clientsName.equals(latestMessage.getSender()) && latestMessage.getText().equals(QUIT_MESSAGE)) {
              client.println(encrypt(QUIT_MESSAGE));
            } else {
              client.println(encrypt(latestMessage.toString()));
            }
          } catch (Exception ex) {
            Report.errorAndGiveUp("Couldn't encrypt");
          }
          currentMessage = clientsListSize - 1;
        } else if (msgText.contains("previous - ")) {
          clientsName = msgText.substring(11);
          clientsList = messageList.get(clientsName);
          try {
            if (currentMessage == -1) {
              client.println(encrypt(NO_MESSAGES));
            } else if (currentMessage > 0) {
            
              Message previousMessage = clientsList.get(--currentMessage);
              client.println(encrypt(previousMessage.toString()));
            } else {
            
              client.println(encrypt(NO_PREVIOUS));
              Message currentMessageText = clientsList.get(currentMessage);
              client.println(encrypt(currentMessageText.toString()));
            }
          } catch (Exception ex) {
            Report.errorAndGiveUp("Couldn't encrypt");
          }
        } else if (msgText.contains("next - ")) {
          clientsName = msgText.substring(7);
          clientsList = messageList.get(clientsName);
          clientsListSize = clientsList.size();
          try {
            if (currentMessage == -1) {
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
            Report.errorAndGiveUp("Couldn't encrypt");
          }
        } else if (msgText.contains("delete - ")) {
          clientsName = msgText.substring(9);
          clientsList = messageList.get(clientsName);
          clientsListSize = clientsList.size();
          try {
            if (currentMessage == -1) {
              client.println(encrypt(NO_MESSAGES));
            } else if (currentMessage == 0) { 
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
            Report.errorAndGiveUp("Couldn't encrypt");
          }
          
        } else if (msgText.contains("login - ")) {
          try {
            client.println(encrypt(LOGGED_IN));
            clientsName = msgText.substring(8);
            clientsList = messageList.get(clientsName);
            clientsListSize = clientsList.size();
            if (clientsListSize > 0) {
              Message latestMessage = clientsList.get(clientsListSize - 1);
              client.println(encrypt(latestMessage.toString()));
            } else {
              client.println(encrypt(NO_MESSAGES));
            }
            currentMessage = clientsListSize - 1;
          } catch (Exception ex) {
          Report.errorAndGiveUp("Couldn't encrypt");
        }
        } else {
          try {
            client.println(encrypt(msgText));
          } catch (Exception ex) {
            Report.errorAndGiveUp("Couldn't encrypt");
          }
        }
        
        
      }
      catch (InterruptedException e) {
        // Do nothing and go back to the infinite while loop.
      }
    }
  }

  public String encrypt(String plainText) throws Exception {
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);
    byte[] byteEncrypted = cipher.doFinal(plainText.getBytes());
    String stringEncrypted = Base64.getEncoder().encodeToString(byteEncrypted);
    return stringEncrypted;
    
  }
}

/*

 * Throws InterruptedException if interrupted while waiting

 * See https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/BlockingQueue.html#take--

 */
