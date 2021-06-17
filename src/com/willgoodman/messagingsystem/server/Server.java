package com.willgoodman.messagingsystem.server;

import com.willgoodman.messagingsystem.*;

import java.net.*;
import java.io.*;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * Server for a messaging system.
 * There is no provision for ending the server gracefully.  It will end if (and only if) something exceptional happens.
 *
 * Usage: java Server
 * */
public class Server {

  private static final int START_NUM_OF_CLIENTS = 0;
  private static final String STANDARD_CLIENT_NAME = "Client_";

  private static final String CONNECTED = " connected";
  //Error messages:
  private static final String ALGORITHM_DOESNT_EXIST = "Encryption algorithm doesn't exist: ";
  private static final String INVALID_PUBLIC_KEY = "Invalid public key received from client.";
  private static final String LISTEN_ERROR = "Couldn't listen on port ";
  private static final String IO_ERROR = "IO error: ";

  public static void main(String [] args) {

    try {
      KeyPair keyPair = Config.generateKeys();
      KeyFactory keyFactory = KeyFactory.getInstance(Config.ENCRYPTION_ALGORITHM);

      Hashtable<String,User> users = new Hashtable<>();
      Hashtable<String,Queue<Message>> clients = new Hashtable<>();
      int numOfClients = START_NUM_OF_CLIENTS;


      ServerSocket serverSocket = new ServerSocket(Port.number);

      // We loop for ever, as servers usually do.
      while (true) {
        try {
          // Listen to the socket, accepting connections from new clients:
          Socket socket = serverSocket.accept();

          String clientName = STANDARD_CLIENT_NAME + (++numOfClients);
          Report.behaviour(clientName + CONNECTED);

          // This is so that we can use readLine():
          BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));

          // We create and start a new thread to write to the client:
          PrintStream toClient = new PrintStream(socket.getOutputStream());

          // Exchange public keys
          toClient.println(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
          byte[] decodedClientKey = Base64.getDecoder().decode(fromClient.readLine());
          PublicKey clientPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodedClientKey));

          clients.put(clientName, new LinkedList<>());

          // We create and start a new thread to read from the client:
          (new ServerReceiver(clientName, fromClient, keyPair.getPrivate(), users, clients)).start();

          (new ServerSender(clientName, toClient, clientPublicKey, users, clients)).start();

        } catch (IOException ex) {
          Report.error(IO_ERROR + ex.getMessage());
        } catch (InvalidKeySpecException ex) {
          Report.error(INVALID_PUBLIC_KEY);
        }
      }

    } catch (NoSuchAlgorithmException ex) {
      Report.errorAndGiveUp(ALGORITHM_DOESNT_EXIST + Config.ENCRYPTION_ALGORITHM);
    } catch (IOException ex) {
      Report.errorAndGiveUp(LISTEN_ERROR + Config.PORT);
    }
  }
}
