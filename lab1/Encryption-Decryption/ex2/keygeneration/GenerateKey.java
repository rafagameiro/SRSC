/**
 * Materiais/Labs para SRSC 17/18, Sem-2
 * hj
 **/

// GenerateKey.java

import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;

public class GenerateKey {

/*****************************************************************
  Inicializacoes:
  Usar as parametrizacoes como se quizer desde que adequadas
  Atencao: tamanhos das chaves corretas para os diversos algoritmos
           criptograficos simetricos
 ****************************************************************/

  // public static final String ALGORITHM = "DESede";
  // public static final Integer KEYSIZE = 168;

  // public static final String ALGORITHM = "Blowfish";
  // public static final Integer KEYSIZE = 448;
  
  // ou qualquer outro ... desde que corretamente ...

  public static final String ALGORITHM = "AES";
  public static final Integer KEYSIZE = 256;
  public static final String KEYRING = "keyring";

  /**
   * main()
   */

  public static void main(String[] args) throws Exception {

    // Geracao da chave pretendida para o algorimo definido

    KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM);
    kg.init(KEYSIZE);
    SecretKey key = kg.generateKey();

    // Guarda no chaveiro em disco

    OutputStream os = new FileOutputStream(KEYRING);
    try {
      os.write(key.getEncoded());
      System.out.println("----------------------------------------------");
      System.out.println("Gerada Chave " +ALGORITHM +" com "+KEYSIZE +" bits ");
      System.out.println("Chave guardada no chaveiro " + KEYRING + "...");
      System.out.println("----------------------------------------------");
    } 
    finally {
      try {
        os.close();
      } catch (Exception e) {

        // ... para tratar a excepcao... 

      } 
    } 
  } 

}









