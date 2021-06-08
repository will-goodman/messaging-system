import java.io.*;
import java.net.*;
import java.security.PrivateKey;
import java.util.Base64;
import javax.crypto.Cipher;

// Gets messages from other clients via the server (by the
// ServerSender thread).

public class ClientReceiver extends Thread {

  private BufferedReader server;
  private final PrivateKey PRIVATE_KEY;
  private static final String ENCRYPTION_ALGORITHM = "RSA";
  private static final String QUIT_MESSAGE = "quit";
  //Error messages
  private static final String SERVER_DIED = "Server seems to have died ";
  private static final String DECRYPT_ERROR = "Couldn't decrypt";

  public ClientReceiver(BufferedReader server, PrivateKey privateKey) {
    this.server = server;
    this.PRIVATE_KEY = privateKey;
  }

  public void run() {
    // Print to the user whatever we get from the server:
    try {
      while (true) {
        String s = "";
        try {
          s = decrypt(server.readLine()); 
        } catch (Exception ex) {
          Report.errorAndGiveUp(DECRYPT_ERROR);
        }
        //ClientReceiver must close before ClientSender, otherwise we would get an I/O Exception
        Thread.yield();
        
        if (s != null) {
            if (s.equals(QUIT_MESSAGE)) {                            
                break;
            } else {
                System.out.println(s);
            }
        } else {
          Report.errorAndGiveUp(SERVER_DIED);
        } 
      }
    }
    catch (Exception e) {
      Report.errorAndGiveUp(SERVER_DIED + e.getMessage());
    }
  }

  private String decrypt(String encrypted) throws Exception {
    byte [] byteEncrypted = Base64.getDecoder().decode(encrypted);
    Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, PRIVATE_KEY);
    byte[] bytePlainText = cipher.doFinal(byteEncrypted);
    return new String (bytePlainText);
  }
}


