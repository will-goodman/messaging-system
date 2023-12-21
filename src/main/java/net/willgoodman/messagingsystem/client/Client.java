package net.willgoodman.messagingsystem.client;// Usage:
//        java Client user-nickname server-hostname
//
// After initializing and opening appropriate sockets, we start two
// client threads, one to send messages, and another one to get
// messages.
//
// A limitation of our implementation is that there is no provision
// for a client to end after we start it. However, we implemented
// things so that pressing ctrl-c will cause the client to end
// gracefully without causing the server to fail.
//
// Another limitation is that there is no provision to terminate when
// the server dies.


import net.willgoodman.messagingsystem.Port;
import net.willgoodman.messagingsystem.Report;

import java.io.*;
import java.net.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

class Client {
  
  private static final int KEY_SIZE = 2048;
  private static final String FILENAME = "./src/main/resources/net/willgoodman/messagingsystem/publicKeys.txt";
  
  public static void main(String[] args) {

    // Check correct usage:
    if (args.length != 1) {
      Report.errorAndGiveUp("Usage: java Client server-hostname");
    }

    // Initialize information:
    //String nickname = args[0];
    String hostname = args[0];

    //if (!nickname.equals("quit")) {
    
        //String hostname = "localhost";
        // Open sockets:
        PrintStream toServer = null;
        BufferedReader fromServer = null;
        Socket server = null;


        String threadName = "";
        try {
            server = new Socket(hostname, Port.number); // Matches AAAAA in Server.java
            toServer = new PrintStream(server.getOutputStream());
            fromServer = new BufferedReader(new InputStreamReader(server.getInputStream()));
            threadName = fromServer.readLine();
        } 
        catch (UnknownHostException e) {
            Report.errorAndGiveUp("Unknown host: " + hostname);
        } 
        catch (IOException e) {
            Report.errorAndGiveUp("The server doesn't seem to be running " + e.getMessage());
        }
  
        PublicKey publicKey = null;
        PrivateKey privateKey = null;
      
        try {
          KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
          keyPairGenerator.initialize(KEY_SIZE);
          KeyPair keyPair = keyPairGenerator.genKeyPair();
          publicKey = keyPair.getPublic();
          privateKey = keyPair.getPrivate(); 
        } catch (NoSuchAlgorithmException e) {
          Report.errorAndGiveUp("Encryption algorithm doesn't exist");
        }

        try {
          BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(FILENAME, true));
          String encodedKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
          bufferedWriter.write(threadName + "-" + encodedKey + "\n");
          bufferedWriter.close();
        } catch (IOException e) {
          Report.errorAndGiveUp("Couldn't write to public key file");
        }

      
        // Tell the server what my nickname is:
        //toServer.println(nickname); // Matches BBBBB in Server.java
     
        // Create two client threads of a diferent nature:
        ClientSender sender = new ClientSender(toServer);
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
            Report.errorAndGiveUp("Something wrong " + e.getMessage());
        }
        catch (InterruptedException e) {
            Report.errorAndGiveUp("Unexpected interruption " + e.getMessage());
        }

    /*} else {
        net.willgoodman.messagingsystem.Report.errorAndGiveUp("User nickname cannot be 'quit'");
    }*/
  }
}
