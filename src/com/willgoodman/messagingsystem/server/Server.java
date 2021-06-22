package com.willgoodman.messagingsystem.server;

import com.willgoodman.messagingsystem.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

  private static final Logger LOGGER = LogManager.getLogger(Server.class);
  private static final int START_NUM_OF_CLIENTS = 0;
  private static final String STANDARD_CLIENT_NAME = "Client_";

  public static void main(String [] args) {

    try {
      KeyPair keyPair = Config.generateKeys();
      KeyFactory keyFactory = KeyFactory.getInstance(Config.ENCRYPTION_ALGORITHM);
      LOGGER.info("Generated KeyPair");

      Hashtable<String,User> users = new Hashtable<>();
      Hashtable<String,Queue<Message>> clients = new Hashtable<>();
      Hashtable<String, String> loggedInUsers = new Hashtable<>();
      int numOfClients = START_NUM_OF_CLIENTS;

      ServerSocket serverSocket = new ServerSocket(Config.PORT);
      LOGGER.info(String.format("Started listening on port %d", Config.PORT));

      // We loop for ever, as servers usually do.
      while (true) {
        try {
          // Listen to the socket, accepting connections from new clients:
          Socket socket = serverSocket.accept();

          String clientName = STANDARD_CLIENT_NAME + (++numOfClients);
          LOGGER.debug(String.format("%s connected", clientName));

          BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          PrintStream toClient = new PrintStream(socket.getOutputStream());
          LOGGER.debug("Connected to client");

          // Exchange public keys
          toClient.println(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
          byte[] decodedClientKey = Base64.getDecoder().decode(fromClient.readLine());
          PublicKey clientPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodedClientKey));
          LOGGER.debug("Exchanged keys");

          clients.put(clientName, new LinkedList<>());

          (new ServerReceiver(clientName, fromClient, keyPair.getPrivate(), users, clients, loggedInUsers)).start();
          (new ServerSender(clientName, toClient, clientPublicKey, users, clients, loggedInUsers)).start();
          LOGGER.debug("Started ServerSender and ServerReceiver threads");

        } catch (IOException ex) {
          LOGGER.error(String.format("Error connecting to client (%s)", ex.getMessage()));
        } catch (InvalidKeySpecException ex) {
          LOGGER.error(String.format("Error decoding client public key: %s", ex.getMessage()));
        }
      }

    } catch (NoSuchAlgorithmException ex) {
      LOGGER.fatal(String.format("Encryption algorithm %s is not available", Config.ENCRYPTION_ALGORITHM));
    } catch (IOException ex) {
      LOGGER.fatal(String.format("Couldn't listen on port %d", Config.PORT));
    } finally {
      System.exit(1);
    }
  }
}
