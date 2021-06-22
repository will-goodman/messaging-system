package com.willgoodman.messagingsystem.client;

import com.willgoodman.messagingsystem.Commands;
import com.willgoodman.messagingsystem.Config;
import com.willgoodman.messagingsystem.Report;

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


// Repeatedly reads text from the user and sends them to the server

/**
 * Reads text entered by the user and forwards the commands on to the Server (ServerReceiver class).
 */
public class ClientSender extends Thread {

    private PrintStream toServer;
    private Cipher encryptCipher;
    private AtomicBoolean disconnect;

    public ClientSender(PrintStream toServer, PublicKey serverPublicKey, AtomicBoolean disconnect) {
        this.toServer = toServer;
        this.disconnect = disconnect;

        try {
            this.encryptCipher = Cipher.getInstance(Config.ENCRYPTION_ALGORITHM);
            this.encryptCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            Report.errorAndGiveUp("Error initialising encryption cipher: " + ex.getMessage());
        } catch (InvalidKeyException ex) {
            Report.errorAndGiveUp("Invalid server public key");
        }
    }

    public void run() {

        // So that we can use the method readLine:
        BufferedReader terminal = new BufferedReader(new InputStreamReader(System.in));

        try {
            String command = "";
            while (!command.equals(Commands.QUIT)) {
                try {
                    command = terminal.readLine();

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
                            System.out.println("Unrecognised command: " + command);
                            break;
                    }

                } catch (IOException ex) {
                    Report.errorAndGiveUp("IO Error: " + ex.getMessage());
                }
            }

            this.toServer.println(encrypt(Commands.QUIT));
            this.toServer.flush();  // ensure all messages are sent before closing connection
            this.toServer.close();
            this.disconnect.set(true);

        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            Report.errorAndGiveUp("Error encrypting message: " + ex.getMessage());
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
        return Base64.getEncoder().encodeToString(this.encryptCipher.doFinal(plainText.getBytes()));
    }
}

