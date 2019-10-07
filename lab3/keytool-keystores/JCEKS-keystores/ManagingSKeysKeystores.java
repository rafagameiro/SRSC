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
import java.util.Base64;

public class ManagingSKeysKeystores {
    public static void main(String[] args) {

	try {
	    KeyStore keyStore = KeyStore.getInstance("JCEKS");
            // Keystore where symmetric keys are stored (type JCEKS)
	    // Remember: you can use other format types for internal
	    // Java-object types (ex., JKS) or standards (ex., PKCS#12,
	    // DER, PKCS#7 ...
	    FileInputStream stream = new FileInputStream("mykeystore.jceks");
            // Password of the keystore: in ths case is "password"
	    keyStore.load(stream, "hjhjhjhj".toCharArray());

            // We decided to store the keys in these entries: mykey1, mykey2
            // Also, we used "password" to protect each entry...
            // See how the keytool was used ...

	    Key key1 = keyStore.getKey("mykey1", "hjhjhjhj".toCharArray());
	    Key key2 = keyStore.getKey("mykey2", "hjhjhjhj".toCharArray());
            System.out.println("key1 and key2 extracted form the keystore ...");
            System.out.println("========");
            System.out.println("key1:");
            System.out.println("========");

            byte[] keybytes1= key1.getEncoded();
            //String kb1=new String(keybytes1);
	    // Better:
            String kb1= Base64.getEncoder().encodeToString(keybytes1);

	    System.out.println("Algorithm: "+ key1.getAlgorithm());
	    System.out.println("Key size (bytes): "+ keybytes1.length);
    	    System.out.println("Format: "+ key1.getFormat());
	    System.out.println("Keybytes Enconded (base64): "+ kb1); 



            System.out.println("========");
            System.out.println("key2:");
            System.out.println("========");

            byte[] keybytes2= key2.getEncoded();
            //String kb2=new String(keybytes2);
            String kb2= Base64.getEncoder().encodeToString(keybytes2);

	    System.out.println("Algorithm: "+ key2.getAlgorithm());
	    System.out.println("Key size (bytes): "+ keybytes2.length);
	    System.out.println("Format: "+ key2.getFormat());
	    System.out.println("Keybytes Enconded (base64): "+ kb2); 

            
   
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

    }
}

