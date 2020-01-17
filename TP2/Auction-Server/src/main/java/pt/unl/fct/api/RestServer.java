package pt.unl.fct.api;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Response;

/**
 * REST API for the Repository service
 * 
 * @author Rafael Gameiro
 * @author Manuella Vieira
 *
 */
@Path(RestServer.PATH)
public interface RestServer {

	static final String PATH = "/server";
    
        /**
         * Method handling HTTP GET requests. The returned object will be sent
         * to the client as "text/plain" media type.
         *
         * @return String that will be returned as a text/plain response.
         */

        @POST
        @Path("/login")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        Response login(String clientId);
 
        @POST
        @Path("/new-client")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        Response newClient(String clientInfo);

        @GET
        @Path("/{clientId}/bids")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        Response listClientBids(@PathParam("clientId") String clientId);

        @GET
        @Path("/{clientId}/active-auctions")
        @Produces(MediaType.APPLICATION_JSON)
        Response listActiveAuctions(@PathParam("clientId") String clientId);

	@GET
	@Path("/auctions/{auctionId}")
	@Produces(MediaType.APPLICATION_JSON)
        Response getAuction(@PathParam("auctionId") String auctionId);
        
        @GET
        @Path("/auctions")
        @Produces(MediaType.APPLICATION_JSON)
        Response listAuctions();

        @GET
        @Path("/auctions?type={type}")
        @Produces(MediaType.APPLICATION_JSON)
        Response listAuctionsByType(@PathParam("type") String type);

        @POST
        @Path("/new-auction")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	Response createAuction(String newAuction);

        @PUT
        @Path("/auctions/{auctionId}/end")
        @Consumes(MediaType.APPLICATION_JSON)
        Response endAuction(@PathParam("auctionId") String auctionId, String client);

        @PUT
        @Path("/auctions/{auctionId}/close")
        @Consumes(MediaType.APPLICATION_JSON)
        Response closeAuction(@PathParam("auctionId") String auctionId, String clientNonce);

        @GET
        @Path("/auctions/{auctionId}/bids")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        Response listAuctionBids(@PathParam("auctionId") String auctionId);
        
        @GET
        @Path("/auctions/{auctionId}/results")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        Response checkAuctionOutcome(@PathParam("auctionId") String auctionId);

        @POST
        @Path("/auctions/{auctionId}/new-bid")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	Response newBid(@PathParam("auctionId") String auctionId, String newBid);
       
}

