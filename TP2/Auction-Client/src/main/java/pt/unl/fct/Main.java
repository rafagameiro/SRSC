package pt.unl.fct;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pt.unl.fct.impl.ClientController;
import pt.unl.fct.impl.ServerSide;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.DateTimeException;

/**
 * Main class.
 *
 */
public class Main {

    private static final String CLIENT_BASE_URI = "https://localhost:9091/";
    private static final String CLIENT_CONFIG = "config/.clientinfo";
    private static final String FILE_NOT_FOUND = "File not found.";
    private static final String SERVER_NOT_ONLINE = "\nServer not online at the moment.\nPlease try again later.\n";
    private static final String INVALID_AUCTION_NAME = "\nInvalid Auction Name.";
    private static final String INVALID_DATE = "\nInvalid Date.";
    private static final String INVALID_AUCTION_TYPE = "\nInvalid Auction Type.";
    private static final String AUCTION_CREATED = "\nAuction %s created with success.";
    private static final String BID_CREATED = "\nBid in Auction %s created with success.";
    private static final String NEW_BID_MSG = "\nChoose the auction you wish to make a bid for: ";
    private static final String CLOSE_AUCTION_MSG = "\nChoose the auction you wish to close: \n";
    private static final String LIST_BIDS_MSG = "\nList bids from auction <enter corresponding number>: ";
    private static final String MESSAGE_WINNER = "The Auction %s was closed!\n You are the winner! Congratulations!";

    private static ClientController clientController = null; 
    private static ServerSide serverSide = null;
    
    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
    	
    	serverSide = new ServerSide();
    	clientController = new ClientController();
    	Scanner scanner = new Scanner(System.in);
    	scanner.useDelimiter("\\n");
        login(scanner);
    	displayOptions(scanner);
    	serverSide.stop();
    	
    }

    /**
     * Reads from a local, the last login information 
     * and sends it to the client
     * After a positive response, it starts the program
     *
     * If the answer is not retrieved, the program will finish
     *
     * @param scanner
     */
    public static void login(Scanner scanner) {
       
        FileReader fr;
        try {
            fr = new FileReader(new File(CLIENT_CONFIG));
            JsonObject clientInfo = (new Gson()).fromJson(fr, JsonObject.class);
            clientController.loginUser(clientInfo.get("username").getAsString(), clientInfo.get("ID").getAsString());

            System.out.println("\nWelcome, " + clientInfo.get("username").getAsString() + "!");     
        } catch(FileNotFoundException e) {
            createUser(scanner);

        } catch(Exception e) {
            System.err.println(SERVER_NOT_ONLINE);
            System.exit(1);
        }
    } 

    /**
     * Register the current user, by sending the typed username
     * The server will generate a unique id so that the user can be indentified
     *
     * @param scanner
     */
    public static void createUser(Scanner scanner) {
    	System.out.println("\nChoose a username: \n");
        System.out.print("> ");
    	String username = scanner.next();
    	String id = clientController.registerUser(username);

        File file = new File(CLIENT_CONFIG);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter fw;

        try {
            file.createNewFile();
            fw = new FileWriter(file);

            JsonObject json = new JsonObject();
            json.addProperty("username", username);
            json.addProperty("ID", id);

            gson.toJson(json, fw);
            fw.close();
        } catch(IOException e) {
            System.err.println(FILE_NOT_FOUND);
        }

        System.out.println("Welcome, " + username + "!");
    }
    
    /**
     * Displays a simple menu so that the user can interact with the application
     */
    public static void displayOptions(Scanner scanner){
	
	int opt = 0;
	while(opt!=8) {
		System.out.println("\n=================================\n"
				+ " 1 - Create New Auction\n"
                                + " 2 - List All Auctions\n"
				+ " 3 - Display Auction's Bids\n"
                                + " 4 - Display Client Bids\n"
				+ " 5 - Make new bid\n"
				+ " 6 - Close Auction\n"
                                + " 7 - Check Auction Outcome\n"
				+ " 8 - Exit\n=================================\n");
                System.out.print("> ");
		opt = scanner.nextInt();
		switch(opt) {
			case 1:
				createAuction(scanner);
				break;
			case 2:
				listAuctionsCommand(scanner);
				break;
			case 3:
				getBids(scanner);
				break;
                        case 4: 
                                getClientBids();
                                break;
			case 5: 
				newBid(scanner);
				break;
			case 6:
				closeAuction(scanner);
                                break;
                        case 7:
                                checkOutcome(scanner);
                                break;
			default:
				break;
			}
		}
    }
    
    /**
     * Lists available auctions and prompts the user to select the desired one
     * @return the auction chosen by the user
     */
    public static JsonObject getAuction(Scanner scanner, String message) {
    	System.out.println("\nAvailable Auctions:\n");
    	JsonArray auctionArray = listAuctions(true, false);
    	System.out.println(message);
        System.out.print("> ");
    	int auctionIndex = scanner.nextInt();
    	return auctionArray.get(auctionIndex-1).getAsJsonObject();
    }
    
    /**
     * Lists all bids from a specific auction
     */
    public static void getBids(Scanner scanner) {
                
        System.out.println("\nAuction State:\n1 - List Open\n2 - List Closed\n");
        System.out.print("> ");
	int type = scanner.nextInt();
        System.out.println();
	boolean auctionState = false;
        switch(type) {
		case 1:
		    auctionState = true;
			break;
		case 2:
		    auctionState = false;
			break;
	}
        JsonArray auctionArray = listAuctions(auctionState, false);

        if( auctionArray.size() != 0 ) {
            System.out.print("\n> ");
            int index = scanner.nextInt();

    	    JsonObject chosenAuction = auctionArray.get(index - 1).getAsJsonObject();
    	    String auctionID = chosenAuction.get("UAID").getAsString();

    	    JsonArray response = (new Gson()).fromJson(clientController.getAuction(auctionID), JsonObject.class).get("bids").getAsJsonArray();

            if(response.size() == 0)
                System.out.println("\nNo bids to display.");
            else {

    	        Iterator<JsonElement> it = response.iterator();
                while(it.hasNext()) {
                    JsonObject elem = it.next().getAsJsonObject();
                    System.out.println("\nAuthor: " + elem.get("author").getAsString());
                    System.out.println("Value: " + elem.get("bid").getAsDouble());
                }
            }
        }
    }

    /**
     * List all bids the user has already did
     *
     */
    public static void getClientBids() {
      
        JsonArray response = clientController.getClientBids();
      
        if(response.size() == 0)
            System.out.println("\nNo bids to display.");
        else {
            System.out.println("\nClient Bids:\n");
            
            Iterator<JsonElement> it = response.iterator();
            while(it.hasNext()) {
                JsonObject elem = it.next().getAsJsonObject();
                String status = elem.get("isOpen").getAsBoolean() ? "OPEN" : "CLOSED";
                System.out.println("Auction: " + elem.get("name").getAsString() + " - " + status);
                Iterator<JsonElement> bidIt = elem.get("bids").getAsJsonArray().iterator();
                while(bidIt.hasNext()) {
                    JsonObject bid = bidIt.next().getAsJsonObject();
                    System.out.println("Value: " + bid.get("value").getAsDouble());
                }
            } 
        }
    }
    
    /**
     * Allows the user to make a new bid to a certain auction
     */
    public static void newBid(Scanner scanner) {
    	
    	JsonObject chosenAuction = getAuction(scanner, NEW_BID_MSG);
    	
    	String auctionID = chosenAuction.get("UAID").getAsString();
    	double highestValue = chosenAuction.get("highestBid").getAsDouble();
    	double bidAmount = 0;
    	boolean isEnglishAuction = chosenAuction.get("type").getAsString().equals("English"); 
    	
    	do {
    	    if(isEnglishAuction)
    		System.err.println("\nAttention! Bid amount must be greater than " + highestValue);
    	    System.out.println("\nEnter bid amount: ");
            System.out.print("> ");
    	    bidAmount = scanner.nextDouble();
    	} while(bidAmount<=highestValue && isEnglishAuction);
    		
    	clientController.newBid(auctionID, bidAmount);

        System.out.println(String.format(BID_CREATED, chosenAuction.get("name").getAsString()));
    }
    
    /***
     * Lists all registered auctions 
     * @param type auction opened or not
     * @return an array with all the auctions
     */
    public static void listAuctionsCommand(Scanner scanner) {
        System.out.println("\nAuction State:\n1 - List Open\n2 - List Closed\n3 - List All");
        System.out.print("> ");
	int type = scanner.nextInt();
        System.out.println();
	boolean auctionState = false;
        boolean listAll = false;
        switch(type) {
		case 1:
		    auctionState = true;
			break;
		case 2:
		    auctionState = false;
			break;
                case 3: 
                    listAll = true;
                        break;
	}

    	
        listAuctions(auctionState, listAll);    
    }
    
   /**
    * Prompts the user for information about an auction they wish to create
    * @param scanner
    */
    public static void createAuction(Scanner scanner) {
    	
        String name = "", description = "", timeLimit = "", auctionType = "";

        do {
    	    System.out.println("\nAuction Name:\n");
            System.out.print("> ");
	    name = scanner.next();
            if(name.isEmpty()) {
                System.err.println(INVALID_AUCTION_NAME);
                continue;
            }
            break;
        } while(true);

	System.out.println("\nAuction Description\n");
        System.out.print("> ");
	description = scanner.next();

        do {
	    System.out.println("\nTime limit (dd/mm/yyyy):\n");
            System.out.print("> ");
	    timeLimit = scanner.next();
            if(!checkDateForm(timeLimit)) {
                System.err.println(INVALID_DATE);
                continue;
            }
            break;
        } while(true);

        do {
	    System.out.println("\nAuction Type:\n1 - English Auction\n2 - Blind Auction\n");
            System.out.print("> ");
	    int type = scanner.nextInt();
            switch(type) {
		    case 1:
		        auctionType = "English";
			    break;
		    case 2:
		        auctionType = "Blind";
		    	    break;
	    }
            if(auctionType.isEmpty()) {
                System.err.println(INVALID_AUCTION_TYPE);
                continue;
            }
            break;
        } while(true);

        clientController.newAuction(name, description, timeLimit, auctionType);
        System.out.println(String.format(AUCTION_CREATED, name));
    }
    
    /**
     * ATENTION: NOT FULLY IMPLEMENTED!
     *
     * Selects Auction and if the user it's not the creator,
     * starts computing the proof-of-work
     * If it is the creator, just sends a request to close the auction
     * @param scanner
     */
    public static void closeAuction(Scanner scanner) {
        System.out.println("\n1 - Auctions Created by the client\n2 - Auctions where the client bidded\n");
        int option = scanner.nextInt();
        switch(option) {
            case 1: 
                    break;
            case 2: 
                    JsonObject chosenAuction = getAuction(scanner, CLOSE_AUCTION_MSG);
    	            String auctionID = chosenAuction.get("UAID").getAsString();
    	            System.out.println("Trying to close auction " + chosenAuction.get("name").getAsString() + "...\n");
    	            clientController.closeAuction(auctionID);
                    break;
        }
    }
    
    /**
     * Display Auctions filtered by the current State (open or not),
     * or if the user wants to display all auctions
     *
     * @param isOpen
     * @param listAll
     */
    private static JsonArray listAuctions(boolean isOpen, boolean listAll) {

        JsonArray auctionArray = clientController.listAuctions(isOpen, listAll);
    	
        if(auctionArray.size() == 0)
            System.err.println("No Auctions to display.");
        else {

            int index=0;
	    Iterator<JsonElement> it = auctionArray.iterator();
	    while(it.hasNext()) {
	        JsonObject elem = it.next().getAsJsonObject();
	        String status = elem.get("isOpen").getAsBoolean() ? "OPEN" : "CLOSED";
	        System.out.println((index+1) + ": " + elem.get("name").getAsString() + ", " +  elem.get("time").getAsString() + ", status: " + status);
	        index++;
	    }
        }
        
        return auctionArray;
    }

    /**
     * Checks winner of an auction
     *
     * @param scanner
     */
    public static void checkOutcome(Scanner scanner) {
        
        System.out.println(); 
        JsonArray auctionArray = listAuctions(false, false);

        if(auctionArray.size() != 0) {
            System.out.print("> ");
            int index = scanner.nextInt();
            
            JsonObject chosenAuction = auctionArray.get(index - 1).getAsJsonObject();       
            JsonObject auctionWinner = (new Gson()).fromJson(clientController.checkOutcome(chosenAuction.get("UAID").getAsString()), JsonObject.class);    

            System.out.println("\nAuction " + chosenAuction.get("name").getAsString());
            System.out.println("Winner: " + auctionWinner.get("name").getAsString());
            System.out.println("Highest value: " + auctionWinner.get("bid").getAsString());
        }
    }

    /**
     * Checks if a date was well written 
     * and if it is date after today's date
     * @param date
     */
    private static boolean checkDateForm(String date) {

        boolean correctValues = true;
        try {
            String [] dateValues = date.split("/");
            if(dateValues.length != 3)
                throw new DateTimeException("error");

            LocalDate dateForm = LocalDate.of(Integer.parseInt(dateValues[2]), Integer.parseInt(dateValues[1]), Integer.parseInt(dateValues[0]));
            LocalDate now = LocalDate.now();

            if(dateForm.isBefore(now))
                throw new DateTimeException("error");

        } catch(DateTimeException e) {
            correctValues = false;
        }

        return correctValues;
    }
}

