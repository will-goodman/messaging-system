import java.net.*;
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
  private PasswordList passwordList;
  private int currentMessage;
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
  private static final int START_MESSAGE = 0;
  private static final String ENCRYPTION_ALGORITHM = "RSA";
  private static final int HALF_SECOND = 500;
  //Output messages:
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
  private static final String DECRYPT_ERROR = "Couldn't decrypt"; 
  private static final String CHANGED_TO = " changed to ";
  private static final String LOGGED_IN = " logged in.";
  private static final String LOGIN_MESSAGE = "login - ";
  private static final String WRONG_PASSWORD = "Wrong password";
  private static final String LOGGED_OUT_MESSAGE = " logged out"; 
  private static final String NEW_MESSAGE = "new message - ";
  private static final String PREVIOUS_MESSAGE = "previous - ";
  private static final String NEXT_MESSAGE = "next - ";
  private static final String DELETE_MESSAGE = "delete - ";

  public ServerReceiver(String n, BufferedReader c, ClientTable t, MessageList m, PrivateKey k, PasswordList p) {
    threadName = n;
    myClientsName = n;
    myClient = c;
    clientTable = t;
    messageList = m;
    PRIVATE_KEY = k;
    passwordList = p;
    loggedIn = false;
  }

  public void run() {
    currentMessage = START_MESSAGE;
    try {
      while (true) {
        String command = "";
        try {
          command = decrypt(myClient.readLine()); 
        } catch (Exception ex) {
          Report.errorAndGiveUp(DECRYPT_ERROR);
        }
        //Depending on which command is received, we may have to await further input
        //Equally, what we do next depends on the input, hence the extensive If statements
        if (!command.equals(QUIT_MESSAGE)) {
          String username = "";
          String password = "";
          if (command.equals(REGISTER) && !loggedIn) {
              try {
                username = decrypt(myClient.readLine());
                password = decrypt(myClient.readLine());
              } catch (Exception ex) {

              }
              if (!messageList.contains(username)) {
                messageList.add(username);
                passwordList.add(username, password);
                clientTable.changeKey(username, myClientsName);
                System.out.println(myClientsName + CHANGED_TO + username);
                myClientsName = username;
                loggedIn = true;
                /* 
                The clients name, and hence their message queue, will be different in the if part    from the else part. Therefore I cannot declare the following three lines outside the if statement
                */
                Message msg = new Message(myClientsName, REGISTERED_MESSAGE);
                BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
                myClientsQueue.offer(msg);
              } else {
                Message msg = new Message(myClientsName, USER_EXISTS);
                BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
                myClientsQueue.offer(msg);
              }
              
          } else if (command.equals(LOGIN)) {
            try {
              username = decrypt(myClient.readLine());
              password = decrypt(myClient.readLine());
            } catch (Exception ex) {

            }
            if (messageList.contains(username) && !loggedIn && !clientTable.contains(username)) {
              if (passwordList.authenticate(username, password)) {
                clientTable.changeKey(username, myClientsName);              
                System.out.println(username + LOGGED_IN);
                myClientsName = username;
                loggedIn = true;
              
              
                Message msg = new Message(myClientsName, LOGIN_MESSAGE + myClientsName);
                BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
                myClientsQueue.offer(msg);
              } else {
                Message msg = new Message(myClientsName, WRONG_PASSWORD);
                BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
                myClientsQueue.offer(msg);
              }
            } else if (clientTable.contains(username)) {
              Message msg = new Message(myClientsName, USER_LOGGED_IN_ALREADY);
              BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
              myClientsQueue.offer(msg);
            } else if (loggedIn){
              Message msg = new Message(myClientsName, ALREADY_LOGGED_IN);
              BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
              myClientsQueue.offer(msg);

            } else {
              Message msg = new Message(myClientsName, USER_DOESNT_EXIST);
              BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
              myClientsQueue.offer(msg);
            }
            
          } else if (command.equals(LOGOUT)) {
            if (loggedIn) {
              clientTable.changeKey(threadName, myClientsName);
              System.out.println(myClientsName + LOGGED_OUT);
              myClientsName = threadName;
              Message msg = new Message(myClientsName, LOGGED_OUT_MESSAGE);
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
                  //If the recipient is currently logged in, we can send the message directly to them as well
                  Message msg2 = new Message(myClientsName, NEW_MESSAGE + username);
                  BlockingQueue<Message> recipientsQueue = clientTable.getQueue(username);
                  recipientsQueue.offer(msg2);
                }
               
              } else {
                Message msg = new Message(myClientsName, RECIPIENT_DOESNT_EXIST);
                BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
                myClientsQueue.offer(msg);
              }
            } else {
              Message msg = new Message(myClientsName, NOONE_LOGGED_IN);
              BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
              myClientsQueue.offer(msg);
            }
          } else if (command.equals(PREVIOUS)) {
      
            Message msg = new Message(myClientsName, PREVIOUS_MESSAGE + myClientsName);
            BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
            myClientsQueue.offer(msg);
          
          } else if (command.equals(NEXT)) {
        
            Message msg = new Message(myClientsName, NEXT_MESSAGE + myClientsName);
            BlockingQueue<Message> myClientsQueue = clientTable.getQueue(myClientsName);
            myClientsQueue.offer(msg);            
       

          } else if (command.equals(DELETE)) {
            Message msg = new Message(myClientsName, DELETE_MESSAGE + myClientsName);
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
                    Thread.sleep(HALF_SECOND);
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

  private String decrypt(String encrypted) throws Exception {
    byte [] byteEncrypted = Base64.getDecoder().decode(encrypted);
    Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, PRIVATE_KEY);
    byte[] bytePlainText = cipher.doFinal(byteEncrypted);
    return new String (bytePlainText);
  }
}

