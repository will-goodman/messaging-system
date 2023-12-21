package com.willgoodman.messagingsystem.client;

import com.willgoodman.messagingsystem.Commands;
import com.willgoodman.messagingsystem.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Reads text entered by the user and forwards the commands on to the Server (ServerReceiver class).
 */
public class ClientSender extends Thread {

    private static final Logger LOGGER = LogManager.getLogger(ClientSender.class);
    private PrintStream toServer;
    private Cipher encryptCipher;
    private AtomicBoolean disconnect;

    /**
     * Constructor
     *
     * @param toServer sends messages to the server
     * @param serverPublicKey key used to encrypt messages to the server
     * @param disconnect boolean storing whether the client should disconnect from the server
     */
    public ClientSender(PrintStream toServer, PublicKey serverPublicKey, AtomicBoolean disconnect) {
        LOGGER.info("Constructing ClientSender");
        this.toServer = toServer;
        this.disconnect = disconnect;

        try {
            this.encryptCipher = Cipher.getInstance(Config.ENCRYPTION_ALGORITHM);
            this.encryptCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
            LOGGER.debug("Created encryption cipher");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            LOGGER.fatal(String.format("Error initialising encryption cipher (%s)", ex.getMessage()));
        } catch (InvalidKeyException ex) {
            LOGGER.fatal("Invalid server public key");
        } finally {
            System.exit(1);
        }
    }

    /**
     * Starts a new thread which sends any commands entered by the user to the server
     */
    public void run() {
        LOGGER.info("Running ClientSender.run()");

        // So that we can use the method readLine:
        BufferedReader terminal = new BufferedReader(new InputStreamReader(System.in));
        LOGGER.info("Created BufferedReader from System.in");

        try {
            String command = "";
            while (!command.equals(Commands.QUIT)) {
                command = terminal.readLine();
                LOGGER.info(String.format("Command: %s", command));

                switch (command) {
                    case Commands.REGISTER:
                    case Commands.LOGIN:
                        String username = terminal.readLine();
                        toServer.println(encrypt(command));
                        toServer.println(encrypt(username));
                        break;
                    case Commands.LOGOUT:
                    case Commands.PREVIOUS:
                    case Commands.NEXT:
                    case Commands.DELETE:
                        toServer.println(encrypt(command));
                        break;
                    case Commands.SEND:
                        username = terminal.readLine();
                        String message = terminal.readLine();
                        toServer.println(encrypt(command));
                        toServer.println(encrypt(username));
                        toServer.println(encrypt(message));
                        break;
                    case Commands.QUIT:
                        break;
                    default:
                        LOGGER.debug(String.format("Unrecognised command: %s", command));
                        break;
                }
            }

            LOGGER.info("Disconnecting");

            this.toServer.println(encrypt(Commands.QUIT));
            this.toServer.flush();  // ensure all messages are sent before closing connection
            this.toServer.close();
            this.disconnect.set(true);
            LOGGER.debug("Disconnected");
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            LOGGER.fatal(String.format("Error encrypting message (%s)", ex.getMessage()));
        } catch (IOException ex) {
            LOGGER.fatal(String.format("Error sending to server (%s)", ex.getMessage()));
        } finally {
            System.exit(1);
        }
    }

    /**
     * Encrypts a message (String) ready to be sent to the Server.
     *
     * @param plainText message (String) to encrypt
     * @return the encrypted cipher text
     * @throws IllegalBlockSizeException if the encryption fails
     * @throws BadPaddingException       if the encryption fails
     */
    private String encrypt(String plainText) throws IllegalBlockSizeException, BadPaddingException {
        LOGGER.info("Running ClientSender.encrypt()");
        return Base64.getEncoder().encodeToString(this.encryptCipher.doFinal(plainText.getBytes()));
    }
}

