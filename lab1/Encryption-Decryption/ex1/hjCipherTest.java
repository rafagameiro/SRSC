/**
 * Materiais/Labs para SRSC 18/19, Sem-2
 * Henrique Domingos
 **/

import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class hjCipherTest {
    public static void main(String args[]) {
        try {

            if (args.length != 3) {
		System.out.println("Usar: hjCipherTest data alg cipherconf");
		System.out.println("  ex: hjCipherTest topsecretdata AES AES/CBC/PKCS5Padding");
		System.out.println("  ex: hjCipherTest topsecretdata RC6 RC6/CTR/NoPadding");
		System.out.println("  ex: hjCipherTest topsecretdata BLOWFISH BLOWFISH/CFB/NoPadding");
                System.exit(-1);
	    }
	    // Primeiro vamos gerar a chave
	    // a maneira de obter a chave pode ser variada: 
	    // podemos ter uma numa keystore ja existente ou podemos 
	    // simplesmente gerar uma ou podemos obte-la atraves der
	    // um protocolo seguro de distribuicao de chaves

            KeyGenerator kg = KeyGenerator.getInstance(args[1]);
            Cipher c = Cipher.getInstance(args[2]);

            Key key = kg.generateKey();

            System.out.println("Cifrar:");	   

            c.init(Cipher.ENCRYPT_MODE, key);
            byte input[] = args[0].getBytes();
            byte encrypted[] = c.doFinal(input);
	    byte iv[] = c.getIV();

            byte[] encryptedBase64 = Base64.getEncoder().encode(encrypted);  

            System.out.println("Ciphertext in Base64: " +new String(encryptedBase64));

            System.out.println("Decifrar: ");	   

            IvParameterSpec dps= new IvParameterSpec(iv);

            c.init(Cipher.DECRYPT_MODE, key, dps);
            byte output[] = c.doFinal(encrypted);
            System.out.println("Plaintext inicial: " +new String (output));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

