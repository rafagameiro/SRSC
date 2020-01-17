package pt.unl.fct.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Manuella Vieira
 * @author Rafael Gameiro
 */

/**
 * Class responsible for the security 
 */
public class SecurityHandler {

	private static final String HASH_ALGORITHM = "SHA-256";
	private static final String PROOF_OF_WORK_REQUIREMENT = "00000";
	private MessageDigest hash;
	
	public SecurityHandler(){
		try {
			hash = MessageDigest.getInstance(HASH_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
        
        /**
         * Returns the hexadecimal value of the hash generated from the data
         * 
         * @param data
         */        
	public String computeHash(byte[] data) {
		hash.update(data);
		return hashToHex(hash.digest());
	}
	
        /**
         * Computes the proof of work given a nonce 
         * If it meets the requeriments returns true,
         * otherwise false
         *
         * @param clientNonce
         * @param blockchainHash
         */
	public boolean checkCryptoPuzzle(int clientNonce, String blockchainHash) {
		
		String data = blockchainHash + clientNonce;
		
		hash.update(data.getBytes());
			
		byte[] resultingHash = hash.digest();
		
		String finalHash = hashToHex(resultingHash);
		
		if(finalHash.endsWith(PROOF_OF_WORK_REQUIREMENT))
		    return true;
		else
		    return false;
	}

	/**
         * Computes the proof of work by trying to find a nonce
         * from which the generated hash has 5 zeros at the end
         *
         * @param blockchainHash
         */
	public int computeProofOfWork(String blockchainHash) {
	
		String finalHash = "";
		String data;
		int nonce = 0;
		
		while(!finalHash.endsWith(PROOF_OF_WORK_REQUIREMENT)){
			nonce++;
			data = blockchainHash + nonce;
			
			finalHash = computeHash(data.getBytes());
		}
		
		System.out.println("You did it!");
		System.out.println("Nonce value: " + nonce);
		return nonce;
	}
	
        /**
         * Converts an hash into an hexadecimal value
         *
         * @param hash
         */
	public String hashToHex(byte[] hash) {
		
		StringBuffer hexHash = new StringBuffer();
		
		for(byte hashByte : hash) {
			String hex = Integer.toHexString(0xFF & hashByte);
			if(hex.length() == 1)
				hex = "0" + hex;
			hexHash.append(hex);
		}
		return hexHash.toString();
	}
	
}
