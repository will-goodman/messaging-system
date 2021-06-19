package com.willgoodman.messagingsystem.server;

import com.willgoodman.messagingsystem.Config;
import com.willgoodman.messagingsystem.Report;

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

    private String clientName;
    private PrintStream toClient;
    private Hashtable<String, User> users;
    private Hashtable<String, Queue<Message>> clients;
    private Hashtable<String, String> loggedInUsers;
    private Cipher encryptCipher;

    public ServerSender(String clientName, PrintStream toClient, PublicKey clientPublicKey, Hashtable<String, User> users,
                        Hashtable<String, Queue<Message>> clients, Hashtable<String, String> loggedInUsers) {
        this.clientName = clientName;
        this.toClient = toClient;
        this.users = users;
        this.clients = clients;
        this.loggedInUsers = loggedInUsers;

        try {
            this.encryptCipher = Cipher.getInstance(Config.ENCRYPTION_ALGORITHM);
            this.encryptCipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            Report.errorAndGiveUp("Error initialising encryption cipher: " + ex.getMessage());
        } catch (InvalidKeyException ex) {
            Report.errorAndGiveUp("Invalid client public key");
        }
    }

    public void run() {
        Message lastSentMessage = null;
        while (this.clients.containsKey(this.clientName)) {
            try {
                if (this.clients.get(this.clientName).size() > 0) {
                    this.toClient.println(encrypt(this.clients.get(this.clientName).remove().toString()));
                }

                if (this.loggedInUsers.containsKey(this.clientName)) {
                    Message userCurrentMessage = this.users.get(this.loggedInUsers.get(this.clientName)).getInbox().getCurrentMessage();
                    if (userCurrentMessage != null && userCurrentMessage != lastSentMessage) {
                        this.toClient.println(encrypt(userCurrentMessage.toString()));
                        lastSentMessage = userCurrentMessage;
                    }
                } else {
                    lastSentMessage = null;
                }
            } catch (IllegalBlockSizeException | BadPaddingException ex) {
                Report.errorAndGiveUp("Error encrypting message: " + ex.getMessage());
            }
        }

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
        return Base64.getEncoder().encodeToString(encryptCipher.doFinal(plainText.getBytes()));
    }
}


