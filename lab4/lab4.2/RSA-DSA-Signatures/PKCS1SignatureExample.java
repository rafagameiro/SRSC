import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;

/**
 * Geracao de uma assinatura de uma mensagem com RSA
 * no esquema PKCS1 
 * Este esquema usa uma assinatura de uma sintese SHA512 da mensagem que
 * se pretende assinar
 * Atencao ao controlo do tamanho das chaves face a sintese parametrizada
 * para a assinatura
 */
public class PKCS1SignatureExample
{
    public static void main(
        String[]    args)
        throws Exception
    {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        
        keyGen.initialize(2048, new SecureRandom());
        
        KeyPair             keyPair = keyGen.generateKeyPair();
        Signature           signature = Signature.getInstance("SHA512withRSA");

	signature.initSign(keyPair.getPrivate());

        byte[] message = new byte[] 
	    { (byte)'a', (byte)'b', (byte)'c' , (byte) 'd' };

        signature.update(message);
        byte[]  sigBytes = signature.sign();
        
        // Verificar - neste caso estamos a obter a chave publica do par mas
	// em geral usamos a chave publica que previamente conhecemos de
	// quem assinou.
	// 
        System.out.println();
        System.out.println("MSG to sign (hex) : " + Utils1.toHex(message));
        System.out.println("MSG size (bytes)  : " + message.length);
        System.out.println("Signature (bytes) : " + Utils1.toHex(sigBytes));
        System.out.println("Sig. Size (bytes) : " + sigBytes.length);


        signature.initVerify(keyPair.getPublic());
        signature.update(message);

       if (signature.verify(sigBytes))
        {
            System.out.println("\nAssinatura validada - reconhecida");
        }
        else
        {
            System.out.println("\nAssinatura nao reconhecida");
        }
    }
}
