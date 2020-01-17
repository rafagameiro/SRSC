package pt.unl.fct.impl;

import java.util.UUID;

import com.google.gson.annotations.*;

/**
 * @author Rafael
 * @author Manuella
 */
public class Block {
    
    @Expose
    @SerializedName("author")
    private String username;

    @Expose
    private double bid;

    @Expose
    private UUID ID;
    
    @Expose
    private String selfHash;
    
    @Expose
    private String combinedHash;

    public Block(String username, double bid) {
        this.username = username;
        this.bid = bid;
    }

    /**
     * Returns the username of the bidder
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the ID of the bidder
     */
    public double getBid() {
        return bid;
    }

    /**
     * Returns the combined hash of the bid
     */
    public String getCombinedHash() {
    	return this.combinedHash;
    }
    
    /**
     * Returns the hash of the bid itself
     */
    public String getSelfHash() {
    	return this.selfHash;
    }
    
    /**
     * Defines the hash of the bid itself
     */
    public void setSelfHash(String hash) {
    	this.selfHash = hash;
    }
    
    /**
     * Defines the combined hash of the bid
     */
    public void setCombinedHash(String hash) {
    	this.combinedHash = hash;
    }
    
    /**
     * Returns the bid essential info in a byte array
     */
    public byte[] toByteArray() {
    	String blockData = this.username + this.bid;
    	return blockData.getBytes();
    }
    
}
