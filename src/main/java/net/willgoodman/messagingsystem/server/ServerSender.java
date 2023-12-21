package com.willgoodman.messagingsystem.server;

import com.willgoodman.messagingsystem.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.security.InvalidKeyException;
import java.util.Hashtable;
import java.security.PublicKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Queue;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Sends all relevant messages to the connected client (or a user logged in to the client)
 */
public class ServerSender extends Thread {

    private static final Logger LOGGER = LogManager.getLogger(ServerSender.class);
    private String clientName;
    private PrintStream toClient;
    private Hashtable<String, User> users;
    private Hashtable<String, Queue<Message>> clients;
    private Hashtable<String, String> loggedInUsers;
    private Cipher encryptCipher;

    /**
     * Constructor
     *
     * @param clientName name of the client this thread serves
     * @param toClient sends messages to the client
     * @param clientPublicKey key used to encrypt messages to the client
     * @param users stores details about all users who have used the server
     * @param clients stores messages to be sent to clients (non-user specific)
     * @param loggedInUsers stores details about which users are logged into which threads
     */
    public ServerSender(String clientName, PrintStream toClient, PublicKey clientPublicKey, Hashtable<String, User> users,
                        Hashtable<String, Queue<Message>> clients, Hashtable<String, String> loggedInUsers) {
        LOGGER.info("Constructing ServerSender");
        this.clientName = clientName;
        this.toClient = toClient;
        this.users = users;
        this.clients = clients;
        this.loggedInUsers = loggedInUsers;

        try {
            this.encryptCipher = Cipher.getInstance(Config.ENCRYPTION_ALGORITHM);
            this.encryptCipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);
            LOGGER.debug("Created encryption cipher");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            LOGGER.fatal(String.format("Error initialising encryption cipher (%s)", ex.getMessage()));
        } catch (InvalidKeyException ex) {
            LOGGER.fatal("Invalid client public key");
        } finally {
            System.exit(1);
        }
    }

    /**
     * Starts a new thread which continually sends messages to the client
     */
    public void run() {
        LOGGER.info("Running ServerSender.run()");
        Message lastSentMessage = null;
        while (this.clients.containsKey(this.clientName)) {
            try {
                if (this.clients.get(this.clientName).size() > 0) {
                    LOGGER.debug(String.format("New message for %s", this.clientName));
                    this.toClient.println(encrypt(this.clients.get(this.clientName).remove().toString()));
                }

                if (this.loggedInUsers.containsKey(this.clientName)) {
                    Message userCurrentMessage = this.users.get(this.loggedInUsers.get(this.clientName)).getInbox().getCurrentMessage();
                    if (userCurrentMessage != null && userCurrentMessage != lastSentMessage) {
                        LOGGER.debug(String.format("New message for user %s", this.loggedInUsers.get(this.clientName)));
                        this.toClient.println(encrypt(userCurrentMessage.toString()));
                        lastSentMessage = userCurrentMessage;
                    }
                } else {
                    lastSentMessage = null;
                }
            } catch (IllegalBlockSizeException | BadPaddingException ex) {
                LOGGER.fatal(String.format("Error encrypting message to %s (%s)", this.clientName, ex.getMessage()));
            } catch (NullPointerException ex) {
                LOGGER.warn(String.format("%s quit mid-execution", this.clientName));
            }
        }

        this.toClient.flush();  // ensure all messages are sent before closing connection
        this.toClient.close();
        LOGGER.debug(String.format("Connection to %s closed", this.clientName));
    }

    /**
     * Encrypts a message (String) ready to be sent to a client.
     *
     * @param plainText message (String) to encrypt
     * @return the encrypted cipher text
     * @throws IllegalBlockSizeException if the encryption fails
     * @throws BadPaddingException       if the encryption fails
     */
    private String encrypt(String plainText) throws IllegalBlockSizeException, BadPaddingException {
        LOGGER.info("Running ServerSender.encrypt()");
        return Base64.getEncoder().encodeToString(encryptCipher.doFinal(plainText.getBytes()));
    }
}


