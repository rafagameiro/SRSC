// Programatically using and managing keys in keystores ...
// Hint: Management and Param. Processes on Keystores
//       Minimal exposure of entries in memory (for good security practice)
//       Always discard from memory secrecy parameters, as soon as you 
//       dont need them ... to minimize exposures against intrusions, 
//       memory instrumentation and monitoring or computer-based memory-attacks.

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.util.Base64;

public class ManagingSKeysKeystores {
    public static void main(String[] args) {

	try {
	    KeyStore keyStore = KeyStore.getInstance("JCEKS");
            // Keystore where symmetric keys are stored (type JCEKS)
	    // Remember: you can use other format types for internal
	    // Java-object types (ex., JKS) or standards (ex., PKCS#12,
	    // DER, PKCS#7 ...
	    FileInputStream stream = new FileInputStream("SMCPKeystore.jceks");
            // Password of the keystore: in ths case is "password"
	    keyStore.load(stream, "changeit".toCharArray());

            // We decided to store the keys in these entries: mykey1, mykey2
            // Also, we used "password" to protect each entry...
            // See how the keytool was used ...
            
            MessageDigest hash = MessageDigest.getInstance("SHA256");

            hash.update("Chat".getBytes());
            hash.update("Of".getBytes());
            hash.update("Secret".getBytes());   
            hash.update("Oriental".getBytes());
            hash.update("Culinary".getBytes());
            
            String ola = Base64.getEncoder().encodeToString(hash.digest()).substring(0,16);
            
	    Key key1 = keyStore.getKey("224.5.6.7:9000", ola.toCharArray());
            System.out.println("key:");
            System.out.println("========");

            byte[] keybytes1= key1.getEncoded();
            //String kb1=new String(keybytes1);
	    // Better:
            String kb1= Base64.getEncoder().encodeToString(keybytes1);

	    System.out.println("Algorithm: "+ key1.getAlgorithm());
	    System.out.println("Key size (bytes): "+ keybytes1.length);
    	    System.out.println("Format: "+ key1.getFormat());
	    System.out.println("Keybytes Enconded (base64): "+ kb1); 

   
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

    }
}

