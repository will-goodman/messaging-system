package com.willgoodman.messagingsystem.client;

import com.willgoodman.messagingsystem.Config;
import com.willgoodman.messagingsystem.Report;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Starts a fresh Client which connects to the server with the provided hostname
 *
 * Usage: java Client server-hostname
 */
class Client {

    private static final String USAGE = "Usage: java Client server-hostname";
    //Error messages:
    private static final String UNKNOWN_HOST = "Unknown host: ";
    private static final String SERVER_NOT_RUNNING = "The server doesn't seem to be running ";
    private static final String ALGORITHM_DOESNT_EXIST = "Encryption algorithm doesn't exist";

    public static void main(String[] args) {

        // Check correct usage:
        if (args.length != 1) {
            Report.errorAndGiveUp(USAGE);
        }

        // Initialize information:
        String hostname = args[0];

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(Config.ENCRYPTION_ALGORITHM);
            keyPairGenerator.initialize(Config.KEY_SIZE);
            KeyPair keyPair = keyPairGenerator.genKeyPair();

            Socket server = new Socket(hostname, Config.PORT);
            PrintStream toServer = new PrintStream(server.getOutputStream());
            BufferedReader fromServer = new BufferedReader(new InputStreamReader(server.getInputStream()));

            // Exchange public keys
            byte[] decodedServerKey = Base64.getDecoder().decode(fromServer.readLine());
            KeyFactory keyFactory = KeyFactory.getInstance(Config.ENCRYPTION_ALGORITHM);
            PublicKey serverPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodedServerKey));
            toServer.println(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));

            (new ClientSender(toServer, serverPublicKey)).start();
            (new ClientReceiver(fromServer, keyPair.getPrivate())).start();

        } catch (NoSuchAlgorithmException ex) {
            Report.errorAndGiveUp(ALGORITHM_DOESNT_EXIST);
        } catch (UnknownHostException ex) {
            Report.errorAndGiveUp(UNKNOWN_HOST + hostname);
        } catch (IOException ex) {
            Report.errorAndGiveUp(SERVER_NOT_RUNNING + ex.getMessage());
        } catch (InvalidKeySpecException ex) {
            Report.errorAndGiveUp("Error decoding server public key: " + ex.getMessage());
        }

    }
}
