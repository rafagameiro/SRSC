package pt.unl.fct.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
	
	public String computeHash(byte[] data) {
		hash.update(data);
		return hashToHex(hash.digest());
	}
	
	public boolean checkCryptoPuzzle(String clientNonce, String blockchainHash) {
		
		String data = blockchainHash + Integer.parseInt(clientNonce);
		
		String finalHash = computeHash(data.getBytes()); 

		if(finalHash.endsWith(PROOF_OF_WORK_REQUIREMENT))
			return true;
		else
			return false;
	}
	
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
