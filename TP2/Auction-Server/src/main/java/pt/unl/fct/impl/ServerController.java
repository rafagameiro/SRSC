package pt.unl.fct.impl;

import java.util.UUID;
import java.util.Iterator;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Entity;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import pt.unl.fct.api.RestServer;

import javax.net.ssl.SSLContext;
import org.glassfish.jersey.SslConfigurator;

public class ServerController implements RestServer {

    private static final String SERVER_TARGET = "http://localhost:8080/repository";
    private static final String KEYSTORE_SERVER_FILE = "config/keystore.jks";
    private static final String KEYSTORE_SERVER_PWD = "changeit";
    private static final String TRUSTSTORE_SERVER_FILE = "config/server.truststore";
    private static final String TRUSTSTORE_SERVER_PWD = "changeit";

    private WebTarget client = null;
    private ClientController cltController;
    private Gson gson;

    public ServerController() {
        client = setClient();
        gson = new Gson();
        cltController = new ClientController();
    }
        
    public Response login(String clientId) {
       
        return client.path("/login").request().post(Entity.entity(clientId, MediaType.APPLICATION_JSON));
    }

    public Response newClient(String clientInfo) {

        return client.path("/new-client").request(MediaType.APPLICATION_JSON).post(Entity.entity(clientInfo, MediaType.APPLICATION_JSON));
    }

    public Response listClientBids(String clientId) {
    	
        return client.path("/" + clientId + "/bids").request(MediaType.APPLICATION_JSON).get();
    }

    public Response listActiveAuctions(String clientId) {
        
        return client.path(clientId + "/auctions/active").request(MediaType.APPLICATION_JSON).get();
    }

    public Response getAuction(String auctionId) {

        return client.path("/auctions/" + auctionId).request(MediaType.APPLICATION_JSON).get();
    }

    public Response listAuctions() {

        return client.path("/auctions").request(MediaType.APPLICATION_JSON).get();
    }

    public Response listAuctionsByType(String type) {

        return client.path("auctions?type=" + type).request(MediaType.APPLICATION_JSON).get();
    }

    public Response createAuction(String newAuction) {

        return client.path("/new-auction").request(MediaType.APPLICATION_JSON).post(Entity.entity(newAuction, MediaType.APPLICATION_JSON));
    }

    public Response endAuction(String auctionId, String client) {

        return null;//client.path("auctions/" + auctionId + "/end").request(MediaType.APPLICATION_JSON).post(Entity.entity(client, MediaType.APPLICATION_JSON));
    }

    public Response closeAuction(String auctionId, String clientNonce) {

    	return client.path("/auctions/" + auctionId + "/close").request().post(Entity.entity(clientNonce, MediaType.APPLICATION_JSON));
    }
    
    public Response listAuctionBids(String auctionId) {

        return client.path("/auctions/" + auctionId + "/bids").request().get();
    }

    public Response checkAuctionOutcome(String auctionId) {
    
        return client.path("/auctions/" + auctionId + "/outcome").request().get();
    }

    public Response newBid(String auctionId, String newBid) {

        Response response = client.path("/new-bid").request(MediaType.APPLICATION_JSON).post(Entity.entity(newBid, MediaType.APPLICATION_JSON));

        if(response.getStatus() == 200) {
                
            JsonObject content = gson.fromJson(response.readEntity(String.class), JsonObject.class);
            String message = generateMessage(content.get("auction").getAsString(), newBid);
            publishMessage(content.get("clients").getAsJsonArray(), auctionId, message);
            return Response.ok().entity(content.get("auction").getAsString()).build();
        }
        
        return Response.status(500).build();
    }

    private String generateMessage(String auction, String newBid) {
    
        JsonObject auctionInfo = gson.fromJson(auction, JsonObject.class);
        JsonObject obj = gson.fromJson(newBid, JsonObject.class);
 
        obj.remove("auction");
        obj.remove("authorId");
        if(obj.has("bid"))
            obj.addProperty("new-bid", "true");
 
        if(obj.get("has-auction").getAsString().equals("false")) {
 
            obj.addProperty("join", "true");
            obj.addProperty("auction-name", auctionInfo.get("name").getAsString());
        } else
            obj.addProperty("auction-name", auctionInfo.get("auction").getAsString());
            obj.remove("has-auction");
 
            if(obj.has("clientNonce"))
                obj.addProperty("proof-of-work", "true");
 
        return gson.toJson(obj);
    }



    private void publishMessage(JsonArray endpoints, String auctionId, String message) {
      
        Iterator<JsonElement> it = endpoints.iterator();
        while(it.hasNext()) {
            cltController.updateAuction(it.next().getAsString(), auctionId, message);
        }
    }

    private WebTarget setClient() {

        final ClientConfig clientConfig = new ClientConfig().connectorProvider(new HttpUrlConnectorProvider());
        Client client = ClientBuilder.newBuilder().withConfig(clientConfig).build();

        return client.target(SERVER_TARGET); 
    }

}

