package pt.unl.fct;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.grizzly.http.server.HttpServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pt.unl.fct.impl.SecurityHandler;

import static org.junit.Assert.assertEquals;

public class MyResourceTest {

    private HttpServer server;
    private WebTarget target;
    
    public static void main(String[] args) {
    	SecurityHandler sec = new SecurityHandler();
    	byte[] result = sec.createReceipt("idid", "nomenome", 4.0);
    	System.out.println(result);
    }
    
    
    @Before
    public void setUp() throws Exception {
        // start the server
        server = Main.startServer(false);
        // create the client
        Client c = ClientBuilder.newClient();
        

        // uncomment the following line if you want to enable
        // support for JSON in the client (you also have to uncomment
        // dependency on jersey-media-json module in pom.xml and Main.startServer())
        // --
        // c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());

        
    }

    /**
    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testGetIt() {
        String responseMsg = target.path("myresource").request().get(String.class);
        assertEquals("Got it!", responseMsg);
    }
    **/
}
