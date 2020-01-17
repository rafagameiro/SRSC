package pt.unl.fct.impl;

import java.util.UUID;

import com.google.gson.annotations.*;

public class Block {
    
    @Expose
    @SerializedName("author")
    private String username;

    @Expose
    @SerializedName("authorId")
    private String usernameId;

    @Expose
    private double bid;

    @Expose
    private UUID ID;
    
    @Expose
    private String selfHash;
    
    @Expose
    private String combinedHash;

    public Block(String username, String usernameId, double bid) {
        this.username = username;
        this.usernameId = usernameId;
        this.bid = bid;
    }

    public String getUsername() {
        return username;
    }

    public String getUsernameId() {
        return usernameId;
    }

    public double getBid() {
        return bid;
    }

    public String getCombinedHash() {
    	return this.combinedHash;
    }
    
    public String getSelfHash() {
    	return this.selfHash;
    }
    
    public void setSelfHash(String hash) {
    	this.selfHash = hash;
    }
    
    public void setCombinedHash(String hash) {
    	this.combinedHash = hash;
    }
    
    public byte[] toByteArray() {
    	String blockData = this.username + this.bid;
    	return blockData.getBytes();
    }
    
}
