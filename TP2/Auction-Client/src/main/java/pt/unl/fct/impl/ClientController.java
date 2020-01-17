package pt.unl.fct.impl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;

import com.google.gson.*;

/**
 *
 * @author Manuella Vieira
 * @author Rafael Gameiro
 */

/**
 * Client Reponsible for the resquests to the Auction Server
 */
public class ClientController {
    
	public static final String BASE_URI = "https://localhost:9090/server";
        public static final String TRUSTSTORE_CLIENT_FILE = "config/client.truststore";
	public static final String TRUSTSTORE_CLIENT_PWD = "changeit";
	public static final String KEYSTORE_CLIENT_FILE = "config/keystore.jks";
	public static final String KEYSTORE_CLIENT_PWD = "changeit";
        public static final String RECEIPT_FILE_PREFIX = "receipts/receipt-";
        public static final String AUCTION_FILE_PREFIX = "config/.auction-";
        public static final String FILE_NOT_FOUND = "File not found.";
	public static String[] SSL_protocol; 
	public static String[] supportedCiphersuites;

	static ConfigReader confReader = new ConfigReader();
	private Client client = null;
        private String clientName = null;
        private UUID clientId = null;
	private Gson gson;
	private SecurityHandler securityHandler;
        private String clientURI;
        public Map<UUID, Auction> auctions;

	public ClientController() {
		client = setClient();
		gson = new Gson().newBuilder().setPrettyPrinting().create();
                auctions = new HashMap<UUID, Auction>();
                clientURI = "https://localhost:9091/";
		securityHandler = new SecurityHandler();
	}
	
        /**
         * Sends a request to the server to list the auctions that match the value of the arguments
         *
         * @param open
         * @param all
         */	
	public JsonArray listAuctions(boolean open, boolean all) {
            String auctions = null;

            String type = open ? "open" : "false";
            
            if(!all)
                auctions = client.target(BASE_URI).path("auctions?type=" + type).request(MediaType.APPLICATION_JSON).get(String.class);
            else
            	auctions = client.target(BASE_URI).path("auctions").request(MediaType.APPLICATION_JSON).get(String.class);

            return gson.fromJson(auctions, JsonArray.class);
	}
	
        /**
         * Sends a request to the server to get an Auction identified with the ID auctionID
         *
         * @param auctionID
         */
	public String getAuction(String auctionID){
		return client.target(BASE_URI).path("auctions/" + auctionID).request(MediaType.APPLICATION_JSON).get(String.class);
	}

        /**
         * Sends a request to the server to get all the bids the client has already made
         *
         */
        public JsonArray getClientBids() {

            String response = client.target(BASE_URI).path(clientId.toString() + "/bids").request().get(String.class);
            
            return gson.fromJson(response, JsonArray.class);
        }
	
        /**
         * Sends a request to the server to create a new auction
         *
         * @param auctionName
         * @param auctionDescription
         * @param time
         * @param type
         */
	public void newAuction(String auctionName, String auctionDescription, String timeLimit, String type) {
		String[] labels = {"name", "description", "author", "time", "type"};
		String[] values = {auctionName, auctionDescription, clientId.toString(), timeLimit, type};
		String newAuction = stringToJson(labels, values);
		Response response = client.target(BASE_URI).path("new-auction").request().post(Entity.entity(newAuction, MediaType.APPLICATION_JSON));
                
	}
	
        /**
         * Sends a request to create new bid in the Auction identified with the ID auctionID
         *
         * @param auctionID
         * @param bidAmount
         */
	public void newBid(String auctionID, double bidAmount) {

	    String newBid = "{\"author\": \"%s\", \"authorId\": \"%s\", \"auction\": \"%s\", \"bid\": %s, \"has-auction\": \"%s\" }";
	    UUID id = UUID.fromString(auctionID);
	    Block bid = new Block(clientName, bidAmount);
	    
            boolean hasAuction = auctions.containsKey(UUID.fromString(auctionID));
            if(hasAuction)
                newBid = String.format(newBid, clientName, clientId.toString(), auctionID, String.valueOf(bidAmount), "true");
            else
                newBid = String.format(newBid, clientName, clientId.toString(), auctionID, String.valueOf(bidAmount), "false");

            Response response = client.target(BASE_URI).path("auctions/" + auctionID + "/new-bid").request().post(Entity.entity(newBid, MediaType.APPLICATION_JSON));
            if(response.getStatus() == 200) {
                
                JsonObject json = gson.fromJson(response.readEntity(String.class), JsonObject.class);
    
                if(!hasAuction) {
                    Auction auction = gson.fromJson(json, Auction.class);
                    auctions.put(id, auction);
                }
                
                bid.setSelfHash(securityHandler.computeHash(bid.toByteArray()));
                String data = auctions.get(id).getHashFromPrevious() + bid.getSelfHash();
                bid.setCombinedHash(securityHandler.computeHash(data.getBytes()));
                auctions.get(id).addBidFromJson(bid);
                
                writeAuction(auctions.get(id), auctionID);
                
                //writeReceipt(json.get("receipt").getAsString(), json.get("blockId").getAsString());
            }
	}
	
        /**
         * Writes an auction json into a file
         *
         * @param auction
         * @param auctionID
         */
	private void writeAuction(Auction auction, String auctionID) {

	    FileWriter fw;
	    try {
                fw = new FileWriter(new File(AUCTION_FILE_PREFIX + auctionID));
                gson.toJson(auction, Auction.class, fw);
                fw.close();
            } catch(IOException e) {
                System.err.println("file not found.");
            }
	}

        /**
         * Generates a Json string using the labels and values passed
         * as arguments
         *
         * @param labels
         * @param values
         */
	private String stringToJson(String[] labels, String[] values) {
		String jsonFormat = "{";
		for(int i=0; i < labels.length; i++) {
			jsonFormat+= "\"" + labels[i] + "\":";
			jsonFormat+= "\"" + values[i] + "\"";
			if(i!=labels.length-1)
				 jsonFormat+= ",";
		}
		jsonFormat+="}";
		
		return jsonFormat;
	}
	
        /**
         * Sends a request to create a new User in the system
         * The new user's ID is sent as a sign of success
         *
         * @param username
         */
	public String registerUser(String username) {
		String[] label = {"username", "endpoint"};
		String[] value = {username, clientURI};
		String json = stringToJson(label, value);
                Response response = client.target(BASE_URI).path("new-client").request().post(Entity.entity(json, MediaType.APPLICATION_JSON));

                JsonObject id = gson.fromJson(response.readEntity(String.class), JsonObject.class); 
                clientId = UUID.fromString(id.get("ID").getAsString());
                clientName = username;

             return clientId.toString();   
	}

        /**
         * Sends a request to login the user into the system
         *
         * @param username
         * @param id
         */
        public void loginUser(String username, String id) {

            String[] label = {"ID", "endpoint"};
            String[] value = {id, clientURI};
            String json = stringToJson(label, value);
            Response response = client.target(BASE_URI).path("login").request().post(Entity.entity(json, MediaType.APPLICATION_JSON));

            clientId = UUID.fromString(id);
            clientName = username;
        }
        
        /**
         * Sends a request to terminate the an Auction
         * Only the auctions created by the client can be terminated
         *
         * @param auctionId
         */
        public void closeAuction(String auctionId) {
        	String lastBlockHash = getLastBlockHash(auctionId);
        	
        	if(lastBlockHash == null)
        		return;
        	int nonce = securityHandler.computeProofOfWork(lastBlockHash);
        	String[] label = {"clientNonce"};
        	String[] value = {Integer.toString(nonce)};
        	
        	String json = stringToJson(label, value);
        	
        	Response response = client.target(BASE_URI).path("/auctions/" + auctionId + "/close").request().put(Entity.entity(json, MediaType.APPLICATION_JSON));
        }

        /**
         * Sends a request to see the winner of a closed auction
         *
         * @param auctionId
         */
        public String checkOutcome(String auctionId) {

            Response response = client.target(BASE_URI).path("/auctions/" + auctionId + "/results").request().get();
        
            return response.readEntity(String.class);
        }
        
        /**
         * Gets the combined hash of the last bid made in an Auction
         *
         * @param auctionId
         */
	private String getLastBlockHash(String auctionId) {
	        
		FileReader fr;
	        try {
	            fr = new FileReader(new File(".auction-" + auctionId + ".json"));
	            JsonObject auction = (new Gson()).fromJson(fr, JsonElement.class).getAsJsonObject();
	            
	            JsonArray bids = auction.get("bids").getAsJsonArray();
	            JsonObject bid = bids.get(bids.size()-1).getAsJsonObject();
	            
	            return bid.get("combinedHash").getAsString(); 
	            
	        } catch (Exception e) {
	            System.err.println("You can only close auctions you've participated in!");
	        }

		return null;
        }
	
		
        /**
         * Writes the received receipt into a file
         *
         * @param receipt
         * @param blockId
         */
        private void writeReceipt(String receipt, String blockId) {
            FileWriter fw;
            try {
                fw = new FileWriter(new File(RECEIPT_FILE_PREFIX + blockId));
                JsonObject obj = new JsonObject();
                obj.addProperty("receipt", receipt);

                gson.toJson(obj, fw);        
                fw.close();
            } catch(IOException e) {
                System.err.println(FILE_NOT_FOUND);
            }

        }

        /**
         * Set ups the client object, in order to comunicate with the server
         */
	private static Client setClient() {
	        
	        SslConfigurator sslConfig = SslConfigurator.newInstance()
	                        .trustStoreFile(TRUSTSTORE_CLIENT_FILE)
	                        .trustStorePassword(TRUSTSTORE_CLIENT_PWD)
	                        .keyStoreFile(KEYSTORE_CLIENT_FILE)
	                        .keyPassword(KEYSTORE_CLIENT_PWD);
	
	        final ClientConfig clientConfig = new ClientConfig().connectorProvider(new HttpUrlConnectorProvider());
	        final SSLContext sslContext = sslConfig.createSSLContext();
	        final Client client = ClientBuilder.newBuilder().withConfig(clientConfig)
	    .sslContext(sslContext).build();
	
	       return client;
	   }

}
