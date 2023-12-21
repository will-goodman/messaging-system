package net.willgoodman.messagingsystem.server;

import net.willgoodman.messagingsystem.Message;
import net.willgoodman.messagingsystem.Report;

import java.io.*;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.security.PrivateKey;
import java.util.Base64;
import javax.crypto.Cipher;

// Gets messages from client and puts them in a queue, for another
// thread to forward to the appropriate client.

public class ServerReceiver extends Thread {
  private String myClientsName;
  private String threadName;
  private BufferedReader myClient;
  private ClientTable clientTable;
  private MessageList messageList;
  private int currentMessage = 0;
  private boolean loggedIn;
  private final PrivateKey PRIVATE_KEY;
  private static final String QUIT_MESSAGE = "quit";
  private static final String REGISTER = "register";
  private static final String LOGIN = "login";
  private static final String LOGOUT = "logout";
  private static final String PREVIOUS = "previous";
  private static final String NEXT = "next";
  private static final String DELETE = "delete";
  private static final String SEND = "send";
  private static final String REGISTERED_MESSAGE = "Registered and logged in";
  private static final String USER_EXISTS = "User already exists";
  private static final String ALREADY_LOGGED_IN = "You are already logged in";
  private static final String USER_LOGGED_IN_ALREADY = "User already logged in";
  private static final String USER_DOESNT_EXIST = "User does not exist";
  private static final String LOGGED_OUT = "Logged out";
  private static final String NOONE_LOGGED_IN = "No user currently logged in";
  private static final String RECIPIENT_DOESNT_EXIST = "Recipient does not exist";
  private static final String THREAD_CANT_SLEEP = "Thread could not sleep";
  private static final String DISCONNECTED = " disconnected";
  private static final String QUIT_ERROR = "Quit message could not be sent back to the client";
  private static final String CLIENT_ERROR = "Something went wrong with the client ";
  

  public ServerReceiver(String n, BufferedReader c, ClientTable t, MessageList m, PrivateKey k) {
    threadName = n;
    myClientsName = n;
    myClient = c;
    clientTable = t;
    messageList = m;
    PRIVATE_KEY = k;
    loggedIn = false;
  }

  public void run() {
    try {
      while (true) {
        String command = "";
        try {
          command = decrypt(myClient.readLine()); // Matches CCCCC in ClientSender.java
        } catch (Exception ex) {
          Report.errorAndGiveUp("Couldn't decrypt");
        }
        //String text = myClient.readLine();      // Matches DDDDD in ClientSender.java
        if (!command.equals(QUIT_MESSAGE)) {
          String username = "";
          if (command.equals(REGISTER) && !loggedIn) {
              try {
                username = decrypt(myClient.readLine());
              } catch (Exception ex) {

              }
              if (!messageList.contains(username)) {
                messageList.add(username);
                clientTable.changeKey(username, myClientsName);
                System.out.println(myClientsName + " changed to " + username);
                myClientsName = username;
                loggedIn = true;
                Message msg = new Message(myClientsName, REGISTERED_MESSAGE);
                BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
                myClientsQueue.offer(msg);
              } else {
                Message msg = new Message(myClientsName, USER_EXISTS);
                BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
                myClientsQueue.offer(msg);
                //System.out.println("User already exists");
                //break;
              }
              
          } else if (command.equals(LOGIN)) {
            try {
              username = decrypt(myClient.readLine());
            } catch (Exception ex) {

            }
            if (messageList.contains(username) && !loggedIn && !clientTable.contains(username)) {
              clientTable.changeKey(username, myClientsName);              
              System.out.println(username + " logged in");
              myClientsName = username;
              loggedIn = true;
              
              
              Message msg = new Message(myClientsName, "login - " + myClientsName);
              BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
              myClientsQueue.offer(msg);
              //Add code for most recent message
            } else if (clientTable.contains(username)) {
              //System.out.println("User already logged in");
              Message msg = new Message(myClientsName, USER_LOGGED_IN_ALREADY);
              BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
              myClientsQueue.offer(msg);
            } else if (loggedIn){
              Message msg = new Message(myClientsName, ALREADY_LOGGED_IN);
              BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
              myClientsQueue.offer(msg);

            } else {
              //System.out.println("User does not exist");
              Message msg = new Message(myClientsName, USER_DOESNT_EXIST);
              BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
              myClientsQueue.offer(msg);
              //break;
            }
            
          } else if (command.equals(LOGOUT)) {
            if (loggedIn) {
              //clientTable.changeKey(originalThreadName, myClientsName);
              clientTable.changeKey(threadName, myClientsName);
              System.out.println(myClientsName + " logged out");
              myClientsName = threadName;
              Message msg = new Message(myClientsName, LOGGED_OUT);
              BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
              myClientsQueue.offer(msg); 
              loggedIn = false;
              
            } else {
              Message msg = new Message(myClientsName, NOONE_LOGGED_IN);
              BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
              myClientsQueue.offer(msg);
            }
            
          } else if (command.equals(SEND)) {
            String message = "";
            try {
              username = decrypt(myClient.readLine());
              message = decrypt(myClient.readLine());
            } catch (Exception ex) {

            }
            //Must check that user is logged in first
            if (loggedIn) {
              if (messageList.contains(username)) {
                      
                Message msg = new Message(myClientsName, message);
                ArrayList<Message> recipientsList = messageList.get(username);
                recipientsList.add(msg);
                if (clientTable.contains(username)) {
                  Message msg2 = new Message(myClientsName, "new message - " + username);
                  BlockingQueue<Message> recipientsQueue = clientTable.getQueue(username);
                  recipientsQueue.offer(msg2);
                }
               
              } else {
                Message msg = new Message(myClientsName, RECIPIENT_DOESNT_EXIST);
                BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
                myClientsQueue.offer(msg);
                //System.out.println("Recipient does not exist");
              }
            } else {
              Message msg = new Message(myClientsName, NOONE_LOGGED_IN);
              BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
              myClientsQueue.offer(msg);
              //System.out.println("No user logged in");
            }
          } else if (command.equals(PREVIOUS)) {
      
            Message msg = new Message(myClientsName, "previous - " + myClientsName);
            BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
            myClientsQueue.offer(msg);
          
          } else if (command.equals(NEXT)) {
        
            Message msg = new Message(myClientsName, "next - " + myClientsName);
            BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
            myClientsQueue.offer(msg);            
       

          } else if (command.equals(DELETE)) {
            Message msg = new Message(myClientsName, "delete - " + myClientsName);
            BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
            myClientsQueue.offer(msg);

            
          }
        
        } else if (command.equals(QUIT_MESSAGE)) {
            Message msg = new Message(threadName, QUIT_MESSAGE);
            BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
            if (myClientsQueue != null) {
                myClientsQueue.offer(msg);
                if (loggedIn) {
                  clientTable.changeKey(threadName, myClientsName);
                  myClientsName = threadName;
                  loggedIn = false;
                }
                //ClientReceiver must close before we remove the client from the ClientTable, otherwise ClientReceiver will not be able to receive the "quit" message
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Report.error(THREAD_CANT_SLEEP);
                }
                clientTable.remove(threadName);
                System.out.println(threadName + DISCONNECTED);
            } else {
                Report.error(QUIT_ERROR);
            }
            break;
        } 
        else
          // No point in closing socket. Just give up.
          return;
        }
      } catch (Exception e) {
        Report.error(CLIENT_ERROR
                   + myClientsName + " " + e.getMessage()); 
        // No point in trying to close sockets. Just give up.
        // We end this thread (we don't do System.exit(1)).
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

