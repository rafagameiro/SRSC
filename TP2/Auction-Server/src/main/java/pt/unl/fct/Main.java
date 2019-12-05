package pt.unl.fct;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

/**
 * Main class.
 *
 */
public class Main {
    // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI = "https://localhost:9090/";
    public static final String KEYSTORE_SERVER_FILE = "keystore.jks";
    public static final String KEYSTORE_SERVER_PWD = "changeit";
    public static final String TRUSTSTORE_SERVER_FILE = "server.truststore";
    public static final String TRUSTSTORE_SERVER_PWD = "changeit";

    public static final String[] SSL_PROTOCOLS = {"TLSv1.0","TLSv1.1","TLSv1.2"};

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer(boolean useCliAuth) throws IOException {
        //Creates a SSlContext for the server to use TLS
        SSLContextConfigurator sslContext = new SSLContextConfigurator();

        // set up security context
        sslContext.setKeyStoreFile(KEYSTORE_SERVER_FILE); // contains server keypair
        sslContext.setKeyStorePass(KEYSTORE_SERVER_PWD);

        sslContext.setKeyStoreFile(KEYSTORE_SERVER_FILE); // contains server keypair
        sslContext.setKeyStorePass(KEYSTORE_SERVER_PWD);

        SSLEngineConfigurator sslEngine = new SSLEngineConfigurator(sslContext);

        sslEngine.setNeedClientAuth(useCliAuth);
        sslEngine.setEnabledProtocols(SSL_PROTOCOLS);

        // create a resource config that scans for JAX-RS resources and providers
        // in pt.unl.fct package
        final ResourceConfig rc = new ResourceConfig().packages("pt.unl.fct");

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        final HttpServer grizzlyServer = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc, true, sslEngine, false);

        grizzlyServer.start();
        return grizzlyServer;
    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        HttpServer server = null;
        if(args.length >= 1 && args[0].equals("true"))
            server = startServer(true);
        else
            server = startServer(false);
        System.out.println(String.format("Auction Server started at %s\nHit enter to stop it...", BASE_URI));

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        br.readLine();
        /*Use later in the Auction client
        String teste = null;
        while((teste = br.readLine()).length() != 0) {
            System.out.println(teste);
            teste = "";
        }*/

        server.stop();
    }
}

