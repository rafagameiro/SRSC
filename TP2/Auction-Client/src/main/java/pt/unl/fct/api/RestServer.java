package pt.unl.fct.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(RestServer.PATH)
public interface RestServer {
	
	static final String PATH = "/client";
	
	@POST
        @Path("/update-auction/{auctionID}")
	@Consumes(MediaType.APPLICATION_JSON)
	Response updateAuction(@PathParam("auctionID") String auctionID, String message);
	
}
