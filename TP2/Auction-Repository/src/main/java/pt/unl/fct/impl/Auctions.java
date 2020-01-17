package pt.unl.fct.impl;

import java.io.*;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID; 

import com.google.gson.*;

public class Auctions {

    private static final String FILE_NOT_FOUND = "The auctions file does not exist.";
    private static final String FILE_LOCATION = "config/.auctions";

    private Map<UUID, Auction> auctions;
    private SecurityHandler securityHandler;
    private static Gson gson;
    private static UUID uuid;
    private JsonArray auctionsList;

    public Auctions() {
        gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
        auctions = new HashMap<UUID, Auction>();
        securityHandler = new SecurityHandler();
        loadAuctions();
    }

    public String getClientBids(String clientId) {

        Set<Map.Entry<UUID, Auction>> set = auctions.entrySet();

        String json = "[";
        boolean header = false;
        int i = 1;
        for( Map.Entry<UUID, Auction> entry : set ) {
            String bids = clientBids(entry.getValue(), clientId);
            if(bids.charAt(bids.length()-3) == '[')
                continue;
            json = json.concat(bids);
            if(set.size() > i)
                json = json.concat(", ");
            i++;
        }

        json = json.concat("]");
        return json;
    }

    public String getClientActiveAuctions(String clientId) {

        Set<Map.Entry<UUID, Auction>> set = auctions.entrySet();
        String json = "[";
        for( Map.Entry<UUID, Auction> entry : set ) {
            if(entry.getValue().getAuthor().equals(clientId) && entry.getValue().getIsOpen()) {
            	json = json.concat(entry.getValue().toString());
            	json = json.concat(",");    
            }
        }
        
        json = json.substring(0, json.length()-1);
        json = json.concat("]");

        return json;
    }

    public void addAuction(String newAuction) {
    	UUID id = UUID.randomUUID();
        Auction auction = gson.fromJson(newAuction, Auction.class);
        auction.setID(id);
        auction.setBlockchain();
        auction.setIsOpen(true);
        auctions.put(id, auction);

        JsonElement json = gson.fromJson(gson.toJson(auction), JsonElement.class);
        auctionsList.add(json);
        writeJson();
    }

    public String getAuction(String auctionId) {

        UUID id = UUID.fromString(auctionId);
        Auction auction = auctions.get(id);
       
        return gson.toJson(auction);
    }

    public String getAuctions() {

        Set<Map.Entry<UUID, Auction>> set = auctions.entrySet();

        String json = "[";
        int i = 1;
        for( Map.Entry<UUID, Auction> entry : set ) {
        	json = json.concat(entry.getValue().toString());
                if(set.size() > i)
                    json = json.concat(", ");    
                i++;
        }
        json = json.concat("]");
        return json;
    }

    public String getAuctionsByType(String type) {

        Set<Map.Entry<UUID, Auction>> set = auctions.entrySet();

        boolean auctionStatus = type.compareTo("open") == 0 ? true : false;
        
        String json = "[";
        for( Map.Entry<UUID, Auction> entry : set ) {
            if(entry.getValue().getIsOpen() == auctionStatus) {
            	json = json.concat(entry.getValue().toString());
            	json = json.concat(",");    
            }
        }
        
        if(json.length() > 1)
            json = json.substring(0, json.length()-1);
        json = json.concat("]");

        return json;
    }

    public String addBid(String newBid) {
        
        JsonObject obj = gson.fromJson(newBid, JsonObject.class);
        String auctionId = obj.get("auction").getAsString();
        UUID id = UUID.fromString(auctionId);
        String clientName = obj.get("author").getAsString();
        Block bid = new Block(clientName, obj.get("authorId").getAsString(), obj.get("bid").getAsDouble());
                
        bid.setSelfHash(securityHandler.computeHash(bid.toByteArray()));
        if(auctions.get(id).getChainSize() == 0)
            bid.setCombinedHash(bid.getSelfHash());
        else {

            String data = auctions.get(id).getHashFromPrevious() + bid.getSelfHash();
            bid.setCombinedHash(securityHandler.computeHash(data.getBytes()));
            	
        }
        String auction = "{\"auction\": \"" + auctions.get(id).getName() + "\"}";
        
        if(obj.get("has-auction").getAsString().equals("false"))
            auction = auctions.get(id).getAuctionJson();

        if(auctions.get(id).getType().equals("English"))
            auctions.get(id).setHighestBid(bid.getBid());

        auctions.get(id).addBidFromJson(bid);
        
        JsonElement json = gson.fromJson(gson.toJson(bid), JsonElement.class);
        Iterator<JsonElement> it = auctionsList.iterator();
        while(it.hasNext()) {
            JsonObject elem = it.next().getAsJsonObject();
            if(elem.get("UAID").getAsString().equals(auctionId)) {
                elem.get("bids").getAsJsonArray().add(json);
                if(elem.get("type").getAsString().equals("English")) {
                	elem.remove("highestBid");
                    elem.addProperty("highestBid", bid.getBid());
                }
            }
        }
        writeJson();
        return auction;
    }

    public boolean closeAuction(String auctionId, String clientNonce) {
		
	JsonObject json = gson.fromJson(clientNonce, JsonObject.class);
	String nonce = json.get("clientNonce").getAsString();
	UUID id = UUID.fromString(auctionId);
		
	if(securityHandler.checkCryptoPuzzle(nonce, auctions.get(id).getHashFromPrevious())) {
			
	    auctions.get(id).setIsOpen(false);
			
	    Iterator<JsonElement> it = auctionsList.iterator();
	    while(it.hasNext()) {
	        JsonObject elem = it.next().getAsJsonObject();
	        if(elem.get("UAID").getAsString().equals(auctionId)) {
	            elem.remove("isOpen");
	            elem.addProperty("isOpen", "false");
	        }
	    }
			
	    writeJson();
	    return true;
	}
	else
	    return false;
    }

    public boolean endAuction(String auctionId, String client) {

        Auction auction = auctions.get(UUID.fromString(auctionId));
        JsonObject json = gson.fromJson(client, JsonObject.class);

        if(auction.getAuthor().equals(json.get("ID").getAsString())) {
            auction.setIsOpen(false);
            return true;
        }

        return false;
    }

    public String checkOutcome(String auctionId) {
        
        Auction auction = auctions.get(UUID.fromString(auctionId));
        double highestValue = auction.getHighestBid();
        String winner = "";
        Iterator<Block> it = auction.listBlockChain();
        while(it.hasNext()) {
            Block b = it.next();
            if(b.getBid() == highestValue) {
                winner = b.getUsername();
                break;
            }
        }

        return "{\"name\" : \"" + winner + "\", \"bid\" : \"" + highestValue + "\"}";
    }

    private String clientBids(Auction auction, String client) {

        String bids = "";
        boolean header = true;
        List<Block> bidsList = new LinkedList<Block>();
        Iterator<Block> it = auction.listBlockChain();
        while(it.hasNext()) {
            Block b = it.next();
            if(b.getUsernameId().equals(client))
                bidsList.add(b);

        }
    
        bids = bids.concat("{ \"name\":\"" + auction.getName() + "\", \"isOpen\": \"" + auction.getIsOpen() + "\", \"bids\" : [");
        int count = 1;
        for( Block b : bidsList ) {
            bids = bids.concat("{\"value\": \"" + b.getBid() + "\"}");            
            if(bidsList.size() > count)
                bids = bids.concat(", ");
            count++;
        }

        bids = bids.concat("]}");

        return bids;
    }

    private void loadAuctions() {

        FileReader fr;
        try {
            fr = new FileReader(new File(FILE_LOCATION));
            auctionsList = (new Gson()).fromJson(fr, JsonArray.class);
            
            Iterator<JsonElement> it = auctionsList.iterator();
             
            while(it.hasNext()) {
                JsonObject elem = it.next().getAsJsonObject();
                
                Auction auction = new Auction(elem.get("name").getAsString(), elem.get("description").getAsString(), UUID.fromString(elem.get("author").getAsString()), elem.get("time").getAsString(), elem.get("type").getAsString());
                
                Iterator<JsonElement> itBids = elem.get("bids").getAsJsonArray().iterator();
                while(itBids.hasNext()) {
                 
                    JsonObject bid = itBids.next().getAsJsonObject();
                    Block newBid = gson.fromJson(bid, Block.class);
                    auction.addBidFromJson(newBid);
                }

                UUID id = UUID.fromString(elem.get("UAID").getAsString());
                auction.setID(id);
                double highestBid = elem.get("highestBid").getAsDouble();
                auction.setHighestBid(highestBid);
                boolean isOpen = elem.get("isOpen").getAsBoolean();
                auction.setIsOpen(isOpen);
                
                auctions.put(id, auction);
           }
    

        } catch (Exception e) {
            System.err.println(FILE_NOT_FOUND);
        }
    }

    private void writeJson() {
    
        FileWriter fw;
        try {
            fw = new FileWriter(new File(FILE_LOCATION));
            gson.toJson(auctionsList, fw);
            fw.close();
        } catch(IOException e) {
            System.err.println(FILE_NOT_FOUND);
        }
    }
    
}
