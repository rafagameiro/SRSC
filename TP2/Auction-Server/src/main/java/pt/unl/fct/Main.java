package pt.unl.fct;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import javax.net.ssl.SSLContext;
import java.net.URI;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.util.Scanner;

import pt.unl.fct.impl.ConfigReader;

/**
 * Main class.
 *
 */
public class Main {
    // Base URI the Grizzly HTTP server will listen on
    private static final String AUTH_MODE_QUESTION = "\nAuthentication Mode:\n";
    private static final String AUTH_SERVER = "SERVER-ONLY";
    private static final String MUTUAL_AUTH = "MUTUAL";
    private static final String AUTH_UNKNOWN = "\nAuthentication Mode not recognized.\n";
    private static final String BASE_URI = "https://localhost:9090/";
    private static final String KEYSTORE_SERVER_FILE = "config/keystore.jks";
    private static final String KEYSTORE_SERVER_PWD = "changeit";
    private static final String TRUSTSTORE_SERVER_FILE = "config/server.truststore";
    private static final String TRUSTSTORE_SERVER_PWD = "changeit";
    private static String[] SSL_protocols;
    private static String[] enabledCiphersuites;

    
    static ConfigReader confReader = new ConfigReader();
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

        sslContext.setTrustStoreFile(TRUSTSTORE_SERVER_FILE); // contains server keypair
        sslContext.setTrustStorePass(TRUSTSTORE_SERVER_PWD);

        SSLEngineConfigurator sslEngine = new SSLEngineConfigurator(sslContext);
        
        sslEngine.setWantClientAuth(useCliAuth);
        sslEngine.setEnabledProtocols(SSL_protocols);
        sslEngine.setEnabledCipherSuites(enabledCiphersuites);
        sslEngine.setClientMode(false);
        
        // create a resource config that scans for JAX-RS resources and providers
        // in pt.unl.fct package
        final ResourceConfig rc = new ResourceConfig();
        rc.register(pt.unl.fct.impl.ServerController.class);
        
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

    	try {
	    confReader.parseConfFile();

	} catch (SAXException | ParserConfigurationException e) {
	    e.printStackTrace();
	}

    	enabledCiphersuites = confReader.enabledCiphersuites;
    	SSL_protocols = confReader.supportedTLSv;
    	
        HttpServer server = null;
        
        Scanner scanner = new Scanner(System.in);
        String [] modes = confReader.authenticationMode;
        int i = 1;        
        System.out.println(AUTH_MODE_QUESTION);
        for( String mode : modes )
            System.out.println(i++ + " - " + mode);
        
        System.out.print("\n> ");
        int index = scanner.nextInt();
        System.out.println();
        if(index > modes.length) {
            System.err.println(AUTH_UNKNOWN);
            System.exit(1);
        } else
            switch(modes[index-1]) {
                case AUTH_SERVER:
                        server = startServer(false);
                        break;
                case MUTUAL_AUTH:
                        server = startServer(true);
                        break;
            }

        System.out.println(String.format("Auction Server started at %s\nHit enter to stop it...", BASE_URI));

        System.in.read();
        server.stop();
    }
}


