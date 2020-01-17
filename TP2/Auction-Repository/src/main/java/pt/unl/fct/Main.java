package pt.unl.fct;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.xml.sax.SAXException;

import pt.unl.fct.impl.ConfigReader;
import pt.unl.fct.impl.Repository;

import java.io.IOException;
import java.net.URI;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Main class.
 *
 */
public class Main {

    // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI = "http://localhost:8080/";
    public static final String KEYSTORE_SERVER_FILE = "config/keystore.jks";
    public static final String KEYSTORE_SERVER_PWD = "changeit";
    public static final String TRUSTSTORE_SERVER_FILE = "config/server.trustedstore";
    public static final String TRUSTSTORE_SERVER_PWD = "changeit";
    
    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer(boolean useCliAuth) throws IOException {
        
        // create a resource config that scans for JAX-RS resources and providers
        // in pt.unl.fct package
        final ResourceConfig rc = new ResourceConfig().register(new Repository());

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        final HttpServer grizzlyServer = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc, true);

        //final HttpServer grizzlyServer = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc, true);

        grizzlyServer.start();
        return grizzlyServer;
    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        HttpServer server = startServer(false);
        System.out.println(String.format("Auction Repository started at "
                + "%s\nHit enter to stop it...", BASE_URI));
        System.in.read();
        server.stop();
    }
}

