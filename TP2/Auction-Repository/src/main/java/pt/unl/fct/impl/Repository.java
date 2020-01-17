package pt.unl.fct.impl;

import javax.net.ssl.SSLContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import pt.unl.fct.api.RestRepository;
import pt.unl.fct.impl.Auctions;

/**
 * Root resource (exposed at "repository" path)
 */
public class Repository implements RestRepository {
    
    private Auctions auctions;
    private Clients clients;
    
    public Repository() {

        auctions = new Auctions();
        clients = new Clients();
    }

    public Response login(String clientId) {

        clients.login(clientId);
        return Response.ok().build();
    }

    public Response newClient(String client) {

        return Response.ok().entity(clients.registerClient(client)).build();        
    }
    
    public Response getAuction(String auctionId) {

        return Response.ok().entity(auctions.getAuction(auctionId)).build();
    }

    public Response listClientBids(String clientId) {

        return Response.ok().entity(auctions.getClientBids(clientId)).build();
    }

    public Response listClientActiveAuctions(String clientId) {

        return Response.ok().entity(auctions.getClientActiveAuctions(clientId)).build();
    }

    public Response listAuctions() {

        return Response.ok().entity(auctions.getAuctions()).build();
    }

    public Response listAuctionsByType(String type) {

        return Response.ok().entity(auctions.getAuctionsByType(type)).build();
    }

    public Response createAuction(String newAuction) {
        
        auctions.addAuction(newAuction);
        return Response.ok().build();
    }

    public Response newBid(String newBid) {

        JsonObject obj = new JsonObject();

        clients.addNewBidder(newBid);
        JsonArray endpoints = clients.getEndpoint(newBid);
        String auctionInfo = auctions.addBid(newBid);       
        obj.add("clients", endpoints);
        obj.addProperty("auction", auctionInfo);

        return Response.ok().entity((new Gson()).toJson(obj)).build();
    }

    public Response endAuction(String auctionId, String client) {

        if(auctions.endAuction(auctionId, client))
            return Response.ok().build();

        return Response.status(401).build();
    }

    public Response closeAuction(String auctionId, String clientNonce) {
	auctions.closeAuction(auctionId, clientNonce);
	return Response.ok().build();
    }

    public Response checkAuctionOutcome(String auctionId) {

        return Response.ok().entity(auctions.checkOutcome(auctionId)).build();
    }

}
