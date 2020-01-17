package pt.unl.fct.impl;

import java.io.*;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Clients {

    private static final String FILE_LOCATION = "config/.clients";
    private static final String FILE_NOT_FOUND = "Clients file not found.";

    private Map<UUID, String> loggedIn;
    private Map<UUID, List<UUID>> auctionClients;
    private JsonArray clientsList;
    private Gson gson;

    public Clients() {
        auctionClients = new HashMap<UUID, List<UUID>>();
        loggedIn = new HashMap<UUID, String>();
        gson = new Gson();
        loadClients();
    }

    public String registerClient(String client) {

        JsonObject json = gson.fromJson(client, JsonObject.class);

        UUID id = UUID.randomUUID();
        loggedIn.put(id, json.get("endpoint").getAsString());

        writeClient(json.get("ID").getAsString(), json.get("username").getAsString());

        json.remove("username");
        return gson.toJson(json);
    }

    public void login(String client) {

        JsonObject json = gson.fromJson(client, JsonObject.class);

        UUID id = UUID.fromString(json.get("ID").getAsString());
        loggedIn.put(id, json.get("endpoint").getAsString());
    }

    public JsonArray getEndpoint(String bidInfo) {
        JsonObject json = gson.fromJson(bidInfo, JsonObject.class);
        String auctionId = json.get("auction").getAsString();

        List<UUID> clients = auctionClients.get(UUID.fromString(auctionId));
        JsonArray endpoints = new JsonArray();
        int i = 0;
        for(UUID id : clients)
            if(!json.get("authorId").getAsString().equals(id.toString()))
                endpoints.add(loggedIn.get(id));

        return endpoints;
    }

    public void addNewBidder(String bidInfo) {

        JsonObject newBid = gson.fromJson(bidInfo, JsonObject.class);
        UUID auctionID = UUID.fromString(newBid.get("auction").getAsString());
        UUID clientID = UUID.fromString(newBid.get("authorId").getAsString());
        if(!hasUser(auctionID, clientID.toString())) {
            if(!auctionClients.containsKey(auctionID))
                auctionClients.put(auctionID, new LinkedList<UUID>());
            auctionClients.get(auctionID).add(clientID);
        }
    }

    public void deleteAuction(UUID id) {

        auctionClients.remove(id);
    }

    private boolean hasUser(UUID auctionID, String clientId) {

        UUID clientID = UUID.fromString(clientId);
        if(auctionClients.containsKey(auctionID)) {
            List<UUID> bidders = auctionClients.get(auctionID);
 
            boolean has = false;
            for(UUID id : bidders)
                if(id.compareTo(clientID) == 0) {
                    has = true;
                    break;
                }

            return has;
        } else
            return false;
    }

    private void loadClients() {

        FileReader fr;
        try {
            fr = new FileReader(new File(FILE_LOCATION));
            clientsList = (new Gson()).fromJson(fr, JsonArray.class);

        } catch (Exception e) {
            System.err.println(FILE_NOT_FOUND);
        }
    }

    private void writeClient(String id, String name) {

        JsonObject client = new JsonObject();
        client.addProperty("name", name);
        client.addProperty("UUID", id);

        clientsList.add(client);

        Gson gsonBuild = new GsonBuilder().setPrettyPrinting().create();
        FileWriter fw;
        try {
            fw = new FileWriter(new File(FILE_LOCATION));
 
            gsonBuild.toJson(clientsList, fw);
            fw.close();
        } catch (IOException e) {
            System.err.println(FILE_NOT_FOUND);
        }
    }

}
