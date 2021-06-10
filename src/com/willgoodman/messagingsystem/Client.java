package com.willgoodman.messagingsystem;

// Usage:
//        java Client server-hostname
//


import java.io.*;
import java.net.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Cipher;

class Client {
  
  private static final int KEY_SIZE = 2048;
  private static final String FILENAME = "publicKeys.txt";
  private static final String ENCRYPTION_ALGORITHM = "RSA";
  private static final String USAGE = "Usage: java Client server-hostname";
  //Error messages:  
  private static final String UNKNOWN_HOST = "Unknown host: ";
  private static final String SERVER_NOT_RUNNING = "The server doesn't seem to be running ";
  private static final String ALGORITHM_DOESNT_EXIST = "Encryption algorithm doesn't exist";
  private static final String PUBLIC_KEY_FILE_ERROR = "Couldn't write to public key file";
  private static final String IO_ERROR = "Something wrong ";
  private static final String INTERRUPTED_ERROR = "Unexpected interruption ";
  
  public static void main(String[] args) {

    // Check correct usage:
    if (args.length != 1) {
      Report.errorAndGiveUp(USAGE);
    }

    // Initialize information:
    String hostname = args[0];

    
        
        // Open sockets:
        PrintStream toServer = null;
        BufferedReader fromServer = null;
        Socket server = null;


        String threadName = "";
        try {
            server = new Socket(hostname, Port.number); 
            toServer = new PrintStream(server.getOutputStream());
            fromServer = new BufferedReader(new InputStreamReader(server.getInputStream()));
            threadName = fromServer.readLine();
        } 
        catch (UnknownHostException e) {
            Report.errorAndGiveUp(UNKNOWN_HOST + hostname);
        } 
        catch (IOException e) {
            Report.errorAndGiveUp(SERVER_NOT_RUNNING + e.getMessage());
        }
  
        //Generate a public and private key pair, and write the public key to the text file
        PublicKey publicKey = null;
        PrivateKey privateKey = null;
      
        try {
          KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ENCRYPTION_ALGORITHM);
          keyPairGenerator.initialize(KEY_SIZE);
          KeyPair keyPair = keyPairGenerator.genKeyPair();
          publicKey = keyPair.getPublic();
          privateKey = keyPair.getPrivate(); 
        } catch (NoSuchAlgorithmException e) {
          Report.errorAndGiveUp(ALGORITHM_DOESNT_EXIST);
        }

        try {
          BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(FILENAME, true));
          String encodedKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
          bufferedWriter.write(threadName + "-" + encodedKey + "\n");
          bufferedWriter.close();
        } catch (IOException e) {
          Report.errorAndGiveUp(PUBLIC_KEY_FILE_ERROR);
        }
     
        // Create two client threads of a diferent nature:
        ClientSender sender = new ClientSender(toServer, hostname);
        ClientReceiver receiver = new ClientReceiver(fromServer, privateKey);

        // Run them in parallel:
        sender.start();
        receiver.start();
    
        // Wait for them to end and close sockets.
        try {
            sender.join();
            toServer.close();
            receiver.join();
            fromServer.close();
            server.close();
        }
        catch (IOException e) {
            Report.errorAndGiveUp(IO_ERROR + e.getMessage());
        }
        catch (InterruptedException e) {
            Report.errorAndGiveUp(INTERRUPTED_ERROR + e.getMessage());
        }
  }
}
