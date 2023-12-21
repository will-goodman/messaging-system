package com.willgoodman.messagingsystem.client;

import com.willgoodman.messagingsystem.Config;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Starts a fresh Client which connects to the server with the provided hostname
 *
 * Usage: java Client server-hostname
 */
class Client {

    private static final Logger LOGGER = LogManager.getLogger(Client.class);

    public static void main(String[] args) {
        LOGGER.info("Running Client.main()");
        try {
            String hostname = args[0];
            LOGGER.info(String.format("Hostname: %s", hostname));

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(Config.ENCRYPTION_ALGORITHM);
            keyPairGenerator.initialize(Config.KEY_SIZE);
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            LOGGER.debug("Generated KeyPair");

            Socket server = new Socket(hostname, Config.PORT);
            PrintStream toServer = new PrintStream(server.getOutputStream());
            BufferedReader fromServer = new BufferedReader(new InputStreamReader(server.getInputStream()));
            LOGGER.debug("Connected to server");

            // Exchange public keys
            byte[] decodedServerKey = Base64.getDecoder().decode(fromServer.readLine());
            KeyFactory keyFactory = KeyFactory.getInstance(Config.ENCRYPTION_ALGORITHM);
            PublicKey serverPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodedServerKey));
            toServer.println(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            LOGGER.debug("Exchanged keys");

            // when the user enters the quit command this triggers the ClientReceiver to close
            AtomicBoolean disconnect = new AtomicBoolean();

            (new ClientSender(toServer, serverPublicKey, disconnect)).start();
            (new ClientReceiver(fromServer, keyPair.getPrivate(), disconnect)).start();
            LOGGER.debug("Started ClientSender and ClientReceiver threads");

        } catch (NullPointerException ex) {
            LOGGER.fatal("Usage: java Client server-hostname");
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.fatal(String.format("Encryption algorithm %s is not available", Config.ENCRYPTION_ALGORITHM));
        } catch (UnknownHostException ex) {
            LOGGER.fatal("Host could not be found");
        } catch (IOException ex) {
            LOGGER.fatal(String.format("The server may not be running: %s", ex.getMessage()));
        } catch (InvalidKeySpecException ex) {
            LOGGER.fatal(String.format("Error decoding server public key: %s", ex.getMessage()));
        } finally {
            System.exit(1);
        }
    }
}
