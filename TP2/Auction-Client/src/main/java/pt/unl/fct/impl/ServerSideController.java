package pt.unl.fct.impl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import pt.unl.fct.api.RestServer;

/**
 * Class responsible for attending requests sent to the client application
 * 
 * @author Manuella Vieira
 * @author Rafael Gameiro
 */
public class ServerSideController implements RestServer {
	
        private static final String MESSAGE_DELIMITER = "************************************************";
        private static final String MESSAGE_JOIN = "User %s joined Auction %s!";
        private static final String MESSAGE_BID = "User %s bidded %s in Auction %s.";
        private static final String MESSAGE_CLOSED = "The Auction %s was closed!\n The winner is Client %s with highest bid as %s!";
        private static final String MESSAGE_WINNER = "The Auction %s was closed!\n You are the winner! Congratulations!";
        private static final String AUCTION_FILE_PREFIX = "config/.auction-";
        private static final String AUCTION_NOT_FOUND = "Auction file not found.";
        private static final String PROOF_START = "Verifying Proof-of-work...";
        private static final String PROOF_ACCEPTED = "The proof of work with value %d was confirmed!";
        private static final String PROOF_REFUSED = "The proof of work didn't produced the expected result.";

        private SecurityHandler securityHandler;

        public ServerSideController() {
            
            securityHandler = new SecurityHandler();
        }

	@Override
	public Response updateAuction(String auctionID, String auction) {

            JsonObject json = (new Gson()).fromJson(auction, JsonObject.class);
            System.out.println(MESSAGE_DELIMITER);

            if(json.has("join"))
                System.out.println(String.format(MESSAGE_JOIN, json.get("author").getAsString(), json.get("auction-name").getAsString()));
            
            if(json.has("new-bid")) {
                System.out.println(String.format(MESSAGE_BID, json.get("author").getAsString(), json.get("bid").getAsString(), json.get("auction-name").getAsString()));

                writeBid(auctionID, new Block(json.get("author").getAsString(), json.get("bid").getAsDouble()));
            }
                                
            if(json.has("proof-of-work")) {
                System.out.println(String.format(MESSAGE_CLOSED, json.get("auction-name").getAsString(), json.get("author").getAsString(), json.get("bid").getAsString()));
                checkNonce(auctionID, json.get("clientNonce").getAsInt());
            }

            System.out.println(MESSAGE_DELIMITER);

	    return Response.ok().build();
	}
        
        /**
         * Check the nonce send to the client,
         * And according to that displays a different message
         */
        private void checkNonce(String auctionId, int clientNonce) {
            
            FileReader fr;
            try {
                fr = new FileReader(new File(AUCTION_FILE_PREFIX + auctionId));
                JsonObject auction = (new Gson()).fromJson(fr, JsonObject.class);
                JsonArray bids = auction.get("bids").getAsJsonArray();

                System.out.println(PROOF_START);
                if(securityHandler.checkCryptoPuzzle(clientNonce, bids.get(bids.size()-1).getAsJsonObject().get("combinedHash").getAsString()))
                    System.out.println(PROOF_ACCEPTED);
                else
                    System.err.println(PROOF_REFUSED);

                fr.close();
            } catch (FileNotFoundException e) {
                System.out.println(AUCTION_NOT_FOUND);
            } catch (IOException e) {
                System.out.println(AUCTION_NOT_FOUND);
            }
            
        }

        /**
         * Writes the new bid into the Auction stored locally
         */
        private void writeBid(String auctionId, Block bid) {
            
            File file = new File(AUCTION_FILE_PREFIX + auctionId);
            FileReader fr;
            FileWriter fw;
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try {
                fr = new FileReader(file);
                JsonObject auction = gson.fromJson(fr, JsonObject.class);
                JsonArray bids = auction.get("bids").getAsJsonArray();


                bid.setSelfHash(securityHandler.computeHash(bid.toByteArray()));
                String data = bids.get(bids.size()-1).getAsJsonObject().get("combinedHash").getAsString() + bid.getSelfHash();
                bid.setCombinedHash(securityHandler.computeHash(data.getBytes()));
                auction.get("bids").getAsJsonArray().add(gson.toJson(bid));

                fw = new FileWriter(file);
                gson.toJson(auction, fw);
                 
                fr.close();
                fw.close();
            } catch (FileNotFoundException e) {
                System.err.println(AUCTION_NOT_FOUND);
            } catch (IOException e) {
                System.out.println(AUCTION_NOT_FOUND);
            }

        }

}
