/**
 * Materiais/Labs para SRSC 17/18, Sem-2
 * hj
 **/

// SendEncrypt.java: cifrar e enviar uma mensagem usando um socket TCP

import java.io.*;
import java.net.*;
import java.security.spec.KeySpec;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;


/**
 * Encrypt and send
 */
public class SendEncrypt {

  public static void main(String args[]) throws Exception {
      // Defaults para host e porto destino.
      // Se quizer passe em parametro a seguir

      if (args.length != 2) {
	  System.out.println("Usar: SenEncrypt <hostname> <port>");
          System.exit(-1);
      }

      String desthost= args[0]; // Default;
      Integer destport=Integer.parseInt(args[1]); // Default;

      // Defaults para a Cifra... Se quizer passe em parametro a seguir
      String ciphersuite="AES/CTR/PKCS5Padding";


      byte[] ivBytes= new byte[] {
	    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
	    0x08, 0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15 
            };
      IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);


      System.out.println("\nDestino:" +desthost + " Porto:" +destport);
      System.out.println("Ciphersuite a usar: " 
       + ciphersuite);

      String plaintext="INIT";
      SecretKey key = KeyRing.readSecretKey();

      for(;;)
	  {
	   plaintext = prompt("Mensagem Plaintext: ");
	   if (plaintext.equals("tchau!")) break;
	   byte[] ptextbytes= plaintext.getBytes();

	   System.out.println("--------------------------------------------");
	   System.out.println("Plaintext em HEX: " 
	      + Utils.toHex(ptextbytes, ptextbytes.length));

	   Cipher cipher = Cipher.getInstance(ciphersuite);
	   cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
	   byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

	   System.out.println("Mensagem cifrada a enviar (em HEX)...:");
	   System.out.println(Utils.toHex(ciphertext, ciphertext.length) + " Size: " +ciphertext.length);
	   System.out.println("----------------------------------------------");

	   // Enviar cyphertext por um socket !
	   Socket s = new Socket(desthost, destport);
	   try {
	       DataOutputStream os = new DataOutputStream(s.getOutputStream());
	       os.writeInt(ciphertext.length);
	       os.write(ciphertext);
	       os.close();
	   } 
	   finally {
	       try {
		   s.close();
	       } catch (Exception e) {

		   // ... Se quizer trate aqui a excepcap
	       }
	   }
	  }
      System.exit(0);
  }


  /**
   * Mostra um prompt e captura a resposta numa String.
   */
  public static String prompt(String prompt) throws IOException {
    System.out.print(prompt);
    System.out.flush();
    BufferedReader input = 
      new BufferedReader(new InputStreamReader(System.in));
    String response = input.readLine();
    System.out.println();
    return response;
  } 
}

