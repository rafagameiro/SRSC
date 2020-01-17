package pt.unl.fct.impl;

import java.util.UUID;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import com.google.gson.annotations.*;

import pt.unl.fct.impl.Block;

public class Auction {

    @Expose
    @SerializedName("bids") 
    private List<Block> blockChain;

    @Expose
    @SerializedName("UAID")
    private UUID id;

    @Expose
    private String name;

    @Expose
    @SerializedName("type")
    private String auctionType;
    
    public Auction(UUID id, String name, String time, String type){
        this.id = id;
        this.name = name;
        this.auctionType = type;
        blockChain = new LinkedList<Block>();
    }
    
    /**
     * Adds a new Bid to the Auction
     */
    public void addBid(String username, double bid) {
        blockChain.add(new Block(username, bid));
    }

    /**
     * Adds a new Bid to the Auction
     */
    public void addBidFromJson(Block bid) {
    	blockChain.add(bid);
    }

    /**
     * Returns the hash of the last Bid made in the Auction
     */
    public String getHashFromPrevious() {
	Block previousBlock = blockChain.get(getChainSize()-1);
        System.out.println("OLA??");
    	return previousBlock.getCombinedHash();
    }
    
    /**
     * Returns the name of the Auction
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the ID of the Auction
     */
    public String getID() {
	return this.id.toString();
    }
    
    /**
     * Returns the number of bids the Auction has
     */
    public int getChainSize() {
    	return this.blockChain.size();
    }
    
    /**
     * Returns List of bids
     */
    public List<Block> getBlockChain() {
	return this.blockChain;
    }
	
}
