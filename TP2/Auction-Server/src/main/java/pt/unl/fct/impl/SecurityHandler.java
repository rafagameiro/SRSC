package pt.unl.fct.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class SecurityHandler {

	private static final String HASH_ALGORITHM = "SHA-256";
	private static final String PROOF_OF_WORK_REQUIREMENT = "00000";
	private static final String KEYSTORE_LOCATION = "config/keystore.jks";
	private static final String KEYSTORE_PWD = "changeit";
	private MessageDigest hash;
	
	public SecurityHandler(){
		try {
			hash = MessageDigest.getInstance(HASH_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	public PrivateKey retrievePrivateKey() {
		PrivateKey privateKey = null;
		try {
			FileInputStream is = new FileInputStream(KEYSTORE_LOCATION);
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			try {
				keystore.load(is, KEYSTORE_PWD.toCharArray());
				try {
					Key key = keystore.getKey("localhost", KEYSTORE_PWD.toCharArray());
					if(key instanceof PrivateKey) {
						privateKey = (PrivateKey)key;
					}
				} catch (UnrecoverableKeyException e) {
					System.err.println("Key not recoverable!");
				}
			} catch (NoSuchAlgorithmException | CertificateException | IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException | KeyStoreException e) {
		}
		
		return privateKey;
	}
	
	public byte[] createReceipt(String auctionID, String clientID, double bidAmount) {
		Cipher cipher = null;
		PrivateKey key = this.retrievePrivateKey();
		try {
			cipher = Cipher.getInstance(key.getAlgorithm() + "/None/NoPadding");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		}
		try {
			cipher.init(Cipher.ENCRYPT_MODE, key);
		} catch (InvalidKeyException e) {
			System.err.println("Invalid Key!");
			e.printStackTrace();
		}
		
		String data = auctionID + clientID + bidAmount;
		byte[] finalData = null;
		
		try {
			finalData = cipher.doFinal(data.getBytes());
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return finalData;
	}
	
	
	public String testPublicKey() {
            //TODO not implemented
            return null;		
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
