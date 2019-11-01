import java.security.Key;
import java.security.SecureRandom;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;

public class macTest {

    public static void main(String [] args) throws Exception{

        MessageDigest hash = MessageDigest.getInstance("SHA256");

        hash.update("Chat".getBytes()); 
        hash.update("Of".getBytes()); 
        hash.update("Secret".getBytes()); 
        hash.update("Oriental".getBytes()); 
        hash.update("Culinary".getBytes()); 
        hash.update("SEA".getBytes());

        byte[] result = hash.digest();
        System.out.println("1.SEA Keystore password:");
        System.out.println(Base64.getEncoder().encodeToString(result).substring(0,16));

        
        hash = MessageDigest.getInstance("SHA256");

        hash.update("Chat".getBytes()); 
        hash.update("Of".getBytes()); 
        hash.update("Secret".getBytes()); 
        hash.update("Oriental".getBytes()); 
        hash.update("Culinary".getBytes()); 
        hash.update("MAC".getBytes());

        result = hash.digest();
        System.out.println("1.MAC Keystore password:");
        System.out.println(Base64.getEncoder().encodeToString(result).substring(0,16));


        hash = MessageDigest.getInstance("SHA512");

        hash.update("Secret".getBytes()); 
        hash.update("Chat".getBytes()); 
        hash.update("Of".getBytes()); 
        hash.update("The".getBytes()); 
        hash.update("Long".getBytes()); 
        hash.update("Night".getBytes());
        hash.update("Of".getBytes());
        hash.update("Horrors".getBytes());
        hash.update("SEA".getBytes());

        result = hash.digest();
        System.out.println("2.SEA Keystore password:");
        System.out.println(Base64.getEncoder().encodeToString(result).substring(0,16));


        hash = MessageDigest.getInstance("SHA512");

        hash.update("Secret".getBytes()); 
        hash.update("Chat".getBytes()); 
        hash.update("Of".getBytes()); 
        hash.update("The".getBytes()); 
        hash.update("Long".getBytes()); 
        hash.update("Night".getBytes());
        hash.update("Of".getBytes());
        hash.update("Horrors".getBytes());
        hash.update("MAC".getBytes());

        result = hash.digest();
        System.out.println("2.MAC Keystore password:");
        System.out.println(Base64.getEncoder().encodeToString(result).substring(0,16));


        hash = MessageDigest.getInstance("SHA1");

        hash.update("Secret".getBytes()); 
        hash.update("Chat".getBytes()); 
        hash.update("Of".getBytes()); 
        hash.update("The".getBytes()); 
        hash.update("Long".getBytes()); 
        hash.update("Night".getBytes());
        hash.update("Of".getBytes());
        hash.update("Horrors".getBytes());
        hash.update("SEA".getBytes());

        result = hash.digest();
        System.out.println("3.SEA Keystore password:");
        System.out.println(Base64.getEncoder().encodeToString(result).substring(0,16));

        hash = MessageDigest.getInstance("SHA1");

        hash.update("Secret".getBytes()); 
        hash.update("Chat".getBytes()); 
        hash.update("Of".getBytes()); 
        hash.update("The".getBytes()); 
        hash.update("Long".getBytes()); 
        hash.update("Night".getBytes());
        hash.update("Of".getBytes());
        hash.update("Horrors".getBytes());
        hash.update("MAC".getBytes());

        result = hash.digest();
        System.out.println("3.MAC Keystore password:");
        System.out.println(Base64.getEncoder().encodeToString(result).substring(0,16));


    }


}
