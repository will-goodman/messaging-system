package com.willgoodman.messagingsystem.server;

import com.willgoodman.messagingsystem.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Hashtable;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Queue;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;


/**
 * Receives commands from the client (ClientSender) and performs the appropriate actions
 */
public class ServerReceiver extends Thread {

    private static final Logger LOGGER = LogManager.getLogger(ServerReceiver.class);
    private final String clientName;
    private final BufferedReader fromClient;
    private Hashtable<String, User> users;
    private Hashtable<String, Queue<Message>> clients;
    private Hashtable<String, String> loggedInUsers;
    private Cipher DECRYPT_CIPHER;

    /**
     * Constructor
     *
     * @param clientName    name of the client this thread serves
     * @param fromClient    receives messages/commands from the client
     * @param privateKey    key used to decrypt messages from the client
     * @param users         stores details about all users who have used the server
     * @param clients       stores messages to be sent to clients (non-user specific)
     * @param loggedInUsers stores details about which users are logged into which threads
     */
    public ServerReceiver(String clientName, BufferedReader fromClient, PrivateKey privateKey,
                          Hashtable<String, User> users, Hashtable<String, Queue<Message>> clients,
                          Hashtable<String, String> loggedInUsers) {
        LOGGER.info("Constructing ServerReceiver");
        this.clientName = clientName;
        this.fromClient = fromClient;
        this.users = users;
        this.clients = clients;
        this.loggedInUsers = loggedInUsers;

        try {
            this.DECRYPT_CIPHER = Cipher.getInstance(Config.ENCRYPTION_ALGORITHM);
            this.DECRYPT_CIPHER.init(Cipher.DECRYPT_MODE, privateKey);
            LOGGER.debug("Created decryption cipher");
        } catch (Exception ex) {
            LOGGER.fatal(String.format("Error initialising decryption cipher (%s)", ex.getMessage()));
            System.exit(1);
        }
    }

    /**
     * Starts a new thread which handles any commands received from the client
     */
    public void run() {
        LOGGER.info("Running ServerReceiver.run()");
        String command = "";

        try {
            while (!command.equals(Commands.QUIT)) {

                command = decrypt(this.fromClient.readLine());
                LOGGER.debug(String.format("Command from %s: %s", this.clientName, command));

                switch (command) {
                    case Commands.REGISTER:
                        String username = decrypt(this.fromClient.readLine());
                        if (!this.users.containsKey(username)) {
                            this.users.put(username, new User(username));
                            String message = String.format("User %s created by %s", username, this.clientName);
                            LOGGER.debug(message);
                            this.clients.get(this.clientName).offer(new Message(this.clientName, message));
                        } else {
                            String message = String.format("User %s already exists", username);
                            LOGGER.debug(message);
                            this.clients.get(this.clientName).offer(new Message(this.clientName, message));
                        }
                        break;
                    case Commands.LOGIN:
                        username = decrypt(this.fromClient.readLine());
                        if (this.users.containsKey(username)) {
                            this.loggedInUsers.put(this.clientName, username);
                            String message = String.format("User %s logged into %s", username, this.clientName);
                            LOGGER.debug(message);
                            this.clients.get(this.clientName).offer(new Message(this.clientName, message));
                        } else {
                            String message = String.format("User %s does not exist", username);
                            LOGGER.debug(message);
                            this.clients.get(this.clientName).offer(new Message(this.clientName, message));
                        }
                        break;
                    case Commands.LOGOUT:
                        if (!this.loggedInUsers.containsKey(this.clientName)) {
                            LOGGER.debug("No user currently logged in.");
                            this.clients.get(this.clientName).offer(new Message(this.clientName, "No user currently logged in."));
                        } else {
                            username = this.loggedInUsers.get(this.clientName);
                            this.loggedInUsers.remove(this.clientName);
                            String message = String.format("User %s logged out of %s", username, this.clientName);
                            LOGGER.debug(message);
                            this.clients.get(this.clientName).offer(new Message(this.clientName, message));
                        }
                        break;
                    case Commands.SEND:
                        username = decrypt(this.fromClient.readLine());
                        String message = decrypt(this.fromClient.readLine());
                        if (!this.loggedInUsers.containsKey(this.clientName)) {
                            LOGGER.debug("No user currently logged in.");
                            this.clients.get(this.clientName).offer(new Message(this.clientName, "No user currently logged in."));
                        } else {
                            if (this.users.containsKey(username)) {
                                LOGGER.debug(String.format("User %s (%s) sent message to user %s",
                                        this.loggedInUsers.get(this.clientName), this.clientName, username));
                                this.clients.get(this.clientName).offer(new Message(this.clientName, "Message sent."));
                                this.users.get(username).getInbox().addMessage(new Message(this.loggedInUsers.get(this.clientName), message));
                            } else {
                                message = String.format("User %s does not exist", username);
                                LOGGER.debug(message);
                                this.clients.get(this.clientName).offer(new Message(this.clientName, message));
                            }
                        }
                        break;
                    case Commands.PREVIOUS:
                        if (!this.loggedInUsers.containsKey(this.clientName)) {
                            LOGGER.debug("No user currently logged in.");
                            this.clients.get(this.clientName).offer(new Message(this.clientName, "No user currently logged in."));
                        } else {
                            LOGGER.debug(String.format("User %s moved backwards in inbox", this.loggedInUsers.get(this.clientName)));
                            this.users.get(this.loggedInUsers.get(this.clientName)).getInbox().moveBackwards();
                        }
                        break;
                    case Commands.NEXT:
                        if (!this.loggedInUsers.containsKey(this.clientName)) {
                            LOGGER.debug("No user currently logged in.");
                            this.clients.get(this.clientName).offer(new Message(this.clientName, "No user currently logged in."));
                        } else {
                            LOGGER.debug(String.format("User %s moved forwards in inbox", this.loggedInUsers.get(this.clientName)));
                            this.users.get(this.loggedInUsers.get(this.clientName)).getInbox().moveForwards();
                        }
                        break;
                    case Commands.DELETE:
                        if (!this.loggedInUsers.containsKey(this.clientName)) {
                            LOGGER.debug("No user currently logged in.");
                            this.clients.get(this.clientName).offer(new Message(this.clientName, "No user currently logged in."));
                        } else {
                            LOGGER.debug(String.format("User %s deleted message", this.loggedInUsers.get(this.clientName)));
                            this.users.get(this.loggedInUsers.get(this.clientName)).getInbox().deleteMessage();
                        }
                        break;
                    default:
                        LOGGER.debug(String.format("Unrecognised command (%s) received from %s", command, this.clientName));
                        break;
                }
            }
        } catch (IOException ex) {
            LOGGER.error(String.format("Error reading from %s (%s)", this.clientName, ex.getMessage()));
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            LOGGER.error(String.format("Error decrypting message from %s (%s)", this.clientName, ex.getMessage()));
        }

        LOGGER.debug(String.format("%s quit", this.clientName));

        Thread.yield();  // a message may currently being sent to the client
        this.clients.remove(this.clientName);
        try {
            this.fromClient.close();
        } catch (IOException ex) {
            LOGGER.fatal(String.format("Error closing connection with %s (%s)", this.clientName, ex.getMessage()));
        }
    }

    /**
     * Decrypts a message (String) received from a client.
     *
     * @param cipherText the original Base64 encoded cipher text
     * @return the decrypted String message
     * @throws IllegalBlockSizeException if the decryption fails
     * @throws BadPaddingException       if the decryption fails
     */
    private String decrypt(String cipherText) throws IllegalBlockSizeException, BadPaddingException {
        LOGGER.info("Running ServerReceiver.decrypt()");
        return new String(DECRYPT_CIPHER.doFinal(Base64.getDecoder().decode(cipherText)));
    }

}

