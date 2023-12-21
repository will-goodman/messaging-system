package net.willgoodman.messagingsystem.server;// Usage:
//        java Server
//
// There is no provision for ending the server gracefully.  It will
// end if (and only if) something exceptional happens.


import net.willgoodman.messagingsystem.Port;
import net.willgoodman.messagingsystem.Report;

import java.net.*;
import java.io.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Server {

  private static final String FILENAME = "./src/main/resources/net/willgoodman/messagingsystem/publicKeys.txt";
  private static final int keySize = 2048;

  public static void main(String [] args) {

    PrivateKey privateKey = null;
    PublicKey publicKey = null;	
    
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(keySize);
      KeyPair keyPair = keyPairGenerator.genKeyPair();
      publicKey = keyPair.getPublic();
      privateKey = keyPair.getPrivate();
    } catch (NoSuchAlgorithmException e) {
      Report.errorAndGiveUp("Encryption algorithm doesn't exist");
    }

    try {
      BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(FILENAME));
      String encodedKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
      bufferedWriter.write("localhost-" + encodedKey + "\n");
      bufferedWriter.close();
    } catch (IOException e) {
      Report.errorAndGiveUp("Couldn't write to public key file");
    }

    // This table will be shared by the server threads:
    ClientTable clientTable = new ClientTable();
    MessageList messageList = new MessageList();
    int numOfClients = 0;
    
    ServerSocket serverSocket = null;
    
    try {
      serverSocket = new ServerSocket(Port.number);
    } 
    catch (IOException e) {
      Report.errorAndGiveUp("Couldn't listen on port " + Port.number);
    }
    
    try { 
      // We loop for ever, as servers usually do.
      while (true) {
        // Listen to the socket, accepting connections from new clients:
        Socket socket = serverSocket.accept(); // Matches AAAAA in Client
	
        // This is so that we can use readLine():
        BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // We ask the client what its name is:
        //String clientName = fromClient.readLine(); // Matches BBBBB in Client

        String clientName = "Unknown client " + (++numOfClients);
        Report.behaviour(clientName + " connected");
        
        // We add the client to the table:
        clientTable.add(clientName);
        
        // We create and start a new thread to read from the client:
        (new ServerReceiver(clientName, fromClient, clientTable, messageList, privateKey)).start();

        // We create and start a new thread to write to the client:
        PrintStream toClient = new PrintStream(socket.getOutputStream());
        toClient.println(clientName);
        (new ServerSender(clientTable.getQueue(clientName), toClient, messageList, clientName)).start();
      }
    } 
    catch (IOException e) {
      // Lazy approach:
      Report.error("IO error " + e.getMessage());
      // A more sophisticated approach could try to establish a new
      // connection. But this is beyond the scope of this simple exercise.
    }
  }
}
