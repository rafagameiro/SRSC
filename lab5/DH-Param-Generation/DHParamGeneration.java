import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

public class DHParamGeneration {

    public static void main(String[] argv) throws Exception {

	String algo = "DH"; //Change this to RSA, DSA ... Is similar


        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(algo);
        //KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(algo, "BC");

	keyGenerator.initialize(4096);

	KeyPair kpair = keyGenerator.genKeyPair();
	PrivateKey priKey = kpair.getPrivate();
	PublicKey pubKey = kpair.getPublic();

       
	String frm = priKey.getFormat();
	System.out.println("\nPrivate key format: " + frm);

	frm = pubKey.getFormat();
	System.out.println("\nPublic key format: " + frm);
       	System.out.println("\nPublic key alg.:" + pubKey.getAlgorithm());
	System.out.println("\nDiffie-Helman Public key parameters are:\n" + pubKey);
        System.out.println("\n");

    }
}
