package pt.unl.fct.impl;

import java.util.UUID;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import com.google.gson.annotations.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import pt.unl.fct.impl.Block;

public class Auction {

    @Expose
    @SerializedName("bids") 
    private List<Block> blockChain;

    @Expose
    @SerializedName("author")
    private UUID auctionCreator;
    
    @Expose
    @SerializedName("UAID")
    private UUID id;

    @Expose
    private String name;

    @Expose
    private String description;

    @Expose
    private String time;
    
    @Expose
    @SerializedName("type")
    private String auctionType;
    
    @Expose
    private double highestBid; 
    
    @Expose
    private boolean isOpen = true;
    
    public Auction(String name, String description, UUID author, String time, String type) {
        this.id = null;
        this.name = name;
        this.description = description;
        this.auctionCreator = author;
        this.time = time;
        this.auctionType = type;
        blockChain = new LinkedList<Block>();
    }

    public void addBid(String username, String usernameId, double bid) {
        blockChain.add(new Block(username, usernameId, bid));
    }

    public void addBidFromJson(Block bid) {
    	blockChain.add(bid);
    }

    public String getHashFromPrevious() {
	Block previousBlock = blockChain.get(getChainSize()-1);
    	return previousBlock.getCombinedHash();
    }
    
    public String getName() {
        return name;
    }

    public String getID() {
	return this.id.toString();
    }
    
    public void setID(UUID id) {
        this.id = id;
    }

    public String getAuthor() {
        System.out.println(auctionCreator);
        return auctionCreator.toString();
    }

    public String getTime() {
        return time;
    }
    
    public String getType() {
    	return auctionType;
    }

    public void setBlockchain() {	
        blockChain = new LinkedList<Block>();
    }

    public Iterator<Block> listBlockChain() {
        return blockChain.iterator();
    }

    public boolean getIsOpen() {
    	return isOpen;
    }
    
    public void setIsOpen(boolean open) {
    	this.isOpen = open;
    }
    
    public void setHighestBid(double highestBid) {
    	this.highestBid = highestBid;
    }

    public double getHighestBid() {
	    return highestBid;
    }
    
    public int getChainSize() {
    	return this.blockChain.size();
    }
	
    @Override
    public String toString() {
		return "{\"name\": \"" + name + "\"," + "\"highestBid\" : \"" + highestBid + "\",\"UAID\" : \"" + this.id  + "\",\"time\" : \"" + time + "\", \"type\" : \"" + auctionType + "\", \"isOpen\" : \"" + isOpen +  "\"}";
    }

    /**
     * @return a simplified JSON version of the object, in order to be stored in the client
     */
    public String getAuctionJson() {
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(gson.toJson(this), JsonObject.class);
        Iterator<JsonElement> it = json.get("bids").getAsJsonArray().iterator();
        while(it.hasNext()) {
            it.next().getAsJsonObject().remove("authorId");
        }

        return gson.toJson(json);
    }
}
