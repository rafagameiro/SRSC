
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


/**
 * Cifra simetrica em modo ECB - Electronic Code Block
 * Problema: texto em claro igual implica cifra igual
 */
public class SimpleECBExample
{   
    public static void main(
        String[]    args)
        throws Exception
    {
        byte[]  input = new byte[] { 
                0x01, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 
                0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f
	};

        byte[]	keyBytes = new byte[] { 
                0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, 
                0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xab, 
                (byte)0xcd, (byte)0xef, (byte)0xfa, (byte)0xc9 };
        
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        Cipher  cipher = Cipher.getInstance("AES/ECB/NoPadding", "SunJCE");
        
        System.out.println("key   : " + Utils.toHex(keyBytes));
        System.out.println("input : " + Utils.toHex(input));
        
        // encryption 
        cipher.init(Cipher.ENCRYPT_MODE, key);
        //apenas estamos a considerar o tamanho do input porque estamos a usar ECB
        byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
        int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
        ctLength += cipher.doFinal(cipherText, ctLength);

        System.out.println("cipher: " + Utils.toHex(cipherText, ctLength) + " bytes: " + ctLength);
        
        // decryption 
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plainText = new byte[cipher.getOutputSize(ctLength)];
        int ptLength = cipher.update(cipherText, 0, ctLength, plainText, 0);
        ptLength += cipher.doFinal(plainText, ptLength);
        
        System.out.println("plain : " + Utils.toHex(plainText, ptLength) + " bytes: " + ptLength);
    }
}
