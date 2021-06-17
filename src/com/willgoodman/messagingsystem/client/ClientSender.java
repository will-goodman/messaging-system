package com.willgoodman.messagingsystem.client;

import com.willgoodman.messagingsystem.Commands;
import com.willgoodman.messagingsystem.Config;
import com.willgoodman.messagingsystem.Report;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


// Repeatedly reads text from the user and sends them to the server

public class ClientSender extends Thread {

  private PrintStream toServer;
  private Cipher encryptCipher;

  public ClientSender(PrintStream toServer, PublicKey serverPublicKey) {
    this.toServer = toServer;

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
                        String username = terminal.readLine();
                        toServer.println(encrypt(command));
                        toServer.println(encrypt(username));
                        break;
                    default:
                        break;
                }

            } catch (IOException ex) {
                Report.errorAndGiveUp("IO Error: " + ex.getMessage());
            }
        }

        toServer.println(encrypt(Commands.QUIT));
    } catch (IllegalBlockSizeException | BadPaddingException ex) {
        Report.errorAndGiveUp("Error encrypting message: " + ex.getMessage());
    }
  }

  private String encrypt(String plainText) throws IllegalBlockSizeException, BadPaddingException {
    return Base64.getEncoder().encodeToString(this.encryptCipher.doFinal(plainText.getBytes()));
  }
}

