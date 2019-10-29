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
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Random;

public class IVBytesExperiment {

    static String plaintext = "This is a plain text which need to be encrypted by Java AES 256 GCM Encryption Algorithm";
    public static final int AES_KEY_SIZE = 256;
    public static final int GCM_IV_LENGTH = 12;
    public static final int GCM_TAG_LENGTH = 16;

    public static void main(String[] args) {

	try {
	    KeyStore keyStore = KeyStore.getInstance("JCEKS");
            // Keystore where symmetric keys are stored (type JCEKS)
	    // Remember: you can use other format types for internal
	    // Java-object types (ex., JKS) or standards (ex., PKCS#12,
	    // DER, PKCS#7 ...
	    FileInputStream stream = new FileInputStream("teste.jceks");
            // Password of the keystore: in ths case is "password"
	    keyStore.load(stream, "changeit".toCharArray());

	    Key key1 = keyStore.getKey("key", "changeit".toCharArray());
            System.out.println("key:");
            System.out.println("========");

            byte[] keybytes1= key1.getEncoded();

	    System.out.println("Algorithm: "+ key1.getAlgorithm());
	    System.out.println("Key size (bytes): "+ keybytes1.length);

            byte[] IV = new byte[GCM_IV_LENGTH];
            byte[] IV_nd = new byte[16];
            byte[] IV_rd = new byte[8];
            SecureRandom random = new SecureRandom();
            random.nextBytes(IV);
            random.nextBytes(IV_nd);
            random.nextBytes(IV_rd);


            String[] encode = {"206", "219", "257", "306", "319", "327", "506", "519", "527"};
            // Get Cipher Instance
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        
            // Create SecretKeySpec
            SecretKeySpec keySpec = new SecretKeySpec(keybytes1, "RC6");
        
            // Create GCMParameterSpec
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, IV);
        
            // Initialize Cipher for ENCRYPT_MODE
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
        
            // Perform Encryption
            byte[] cipherText = cipher.doFinal(plaintext.getBytes());

            System.out.println(cipherText);
            
            Random rand = new Random();
            ////////////////////////////////////////////////////////////

            int pos = rand.nextInt(9);

            System.out.println("IV bytes");
            System.out.println("GCM");
            System.out.println(Base64.getEncoder().encodeToString(IV));
            System.out.println("RC6");
            System.out.println(Base64.getEncoder().encodeToString(IV_nd));
            System.out.println("Blowfish");
            System.out.println(Base64.getEncoder().encodeToString(IV_rd));
           
            //Way to decode to byte[] after retrieving from .conf file
            String teste = Base64.getEncoder().encodeToString(IV);
            IV = Base64.getDecoder().decode(new String(teste).getBytes());

            int step = Character.getNumericValue(encode[pos].charAt(0));             

            System.out.println("Pos = " + encode[pos]);

            byte[] oldIV = new byte[GCM_IV_LENGTH];
            System.arraycopy(IV, 0, oldIV, 0, IV.length);
            for(int i = 0; i < oldIV.length; i+= step)
                oldIV[i] ^= encode[pos].charAt(1) ^ encode[pos].charAt(2);
           
            System.out.println("New IV bytes"); 
            System.out.println(Base64.getEncoder().encodeToString(oldIV));

            gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, oldIV);
        
            // Initialize Cipher for ENCRYPT_MODE
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
        
            // Perform Encryption
            cipherText = cipher.doFinal(plaintext.getBytes());
            System.out.println(cipherText);
            
            ///////////////////////////////////////////////////////////////////////
            pos = rand.nextInt(9);
            step = Character.getNumericValue(encode[pos].charAt(0)); 

            System.out.println("Pos = " + encode[pos]);

            System.arraycopy(IV, 0, oldIV, 0, IV.length);
            for(int i = 0; i < oldIV.length; i+= step)
                 oldIV[i] ^= encode[pos].charAt(1) ^ encode[pos].charAt(2);
           
            System.out.println("New IV bytes"); 
            System.out.println(Base64.getEncoder().encodeToString(oldIV));


            gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, oldIV);
        
            // Initialize Cipher for ENCRYPT_MODE
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
        
            // Perform Encryption
            cipherText = cipher.doFinal(plaintext.getBytes());
            System.out.println(cipherText);


            /////////////////////////////////////////////////////////////////////
            pos = rand.nextInt(9);
            step = Character.getNumericValue(encode[pos].charAt(0)); 
            
            System.out.println("Pos = " + encode[pos]);

            System.arraycopy(IV, 0, oldIV, 0, IV.length);
            for(int i = 0; i < oldIV.length; i+= step)
                 oldIV[i] ^= encode[pos].charAt(1) ^ encode[pos].charAt(2);
           

            System.out.println("New IV bytes");
            System.out.println(Base64.getEncoder().encodeToString(oldIV));

            gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, oldIV);
        
            // Initialize Cipher for ENCRYPT_MODE
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
        
            // Perform Encryption
            cipherText = cipher.doFinal(plaintext.getBytes());

            System.out.println(cipherText);

            gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, oldIV);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
            
            System.out.println(new String(cipher.doFinal(cipherText)));
            

	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

    }
}

