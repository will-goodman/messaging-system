// Usage:
//        java Server
//
// There is no provision for ending the server gracefully.  It will
// end if (and only if) something exceptional happens.

package com.willgoodman.messagingsystem;

import java.net.*;
import java.io.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.PrivateKey;
import javax.crypto.Cipher;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Server {

  private static final String FILENAME = "publicKeys.txt";
  private static final int keySize = 2048;
  private static final String ENCRYPTION_ALGORITHM = "RSA";
  private static final int START_NUM_OF_CLIENTS = 0;
  private static final String STANDARD_CLIENT_NAME = "Unknown client ";
  private static final String CONNECTED = " connected";
  private static final String SERVER_NAME = "localhost";
  //Error messages:
  private static final String ALGORITHM_DOESNT_EXIST = "Encryption algorithm doesn't exist";
  private static final String PUBLIC_KEY_FILE_ERROR = "Couldn't write to public key file";
  private static final String LISTEN_ERROR = "Couldn't listen on port ";
  private static final String IO_ERROR = "IO error ";

  public static void main(String [] args) {

    //Generate a public and private key pair, and write the public key to the text file
    PrivateKey privateKey = null;
    PublicKey publicKey = null;	
    
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ENCRYPTION_ALGORITHM);
      keyPairGenerator.initialize(keySize);
      KeyPair keyPair = keyPairGenerator.genKeyPair();
      publicKey = keyPair.getPublic();
      privateKey = keyPair.getPrivate();
    } catch (NoSuchAlgorithmException e) {
      Report.errorAndGiveUp(ALGORITHM_DOESNT_EXIST);
    }

    try {
      BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(FILENAME));
      String encodedKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
      bufferedWriter.write(SERVER_NAME + "-" + encodedKey + "\n");
      bufferedWriter.close();
    } catch (IOException e) {
      Report.errorAndGiveUp(PUBLIC_KEY_FILE_ERROR);
    }

    // This table will be shared by the server threads:
    ClientTable clientTable = new ClientTable();
    MessageList messageList = new MessageList();
    PasswordList passwordList = new PasswordList();
    int numOfClients = START_NUM_OF_CLIENTS;
    
    ServerSocket serverSocket = null;
    
    try {
      serverSocket = new ServerSocket(Port.number);
    } 
    catch (IOException e) {
      Report.errorAndGiveUp(LISTEN_ERROR + Port.number);
    }
    
    try { 
      // We loop for ever, as servers usually do.
      while (true) {
        // Listen to the socket, accepting connections from new clients:
        Socket socket = serverSocket.accept(); 
	
        // This is so that we can use readLine():
        BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));


        String clientName = STANDARD_CLIENT_NAME + (++numOfClients);
        Report.behaviour(clientName + CONNECTED);
        
        // We add the client to the table:
        clientTable.add(clientName);
        
        // We create and start a new thread to read from the client:
        (new ServerReceiver(clientName, fromClient, clientTable, messageList, privateKey, passwordList)).start();

        // We create and start a new thread to write to the client:
        PrintStream toClient = new PrintStream(socket.getOutputStream());
        toClient.println(clientName);
        (new ServerSender(clientTable.getQueue(clientName), toClient, messageList, clientName)).start();
      }
    } 
    catch (IOException e) {
      
      Report.error(IO_ERROR + e.getMessage());
    }
  }
}
