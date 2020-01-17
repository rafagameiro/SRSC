package pt.unl.fct.impl;

import java.io.IOException;
import java.net.URI;

import javax.xml.parsers.ParserConfigurationException;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.xml.sax.SAXException;

/**
 *
 * @author Manuella Vieira
 * @author Rafael Gameiro
 */

/**
 * Class responsible for the initialization of the Client Server, 
 * reponsible to receive notifications about the auctions it has bidded
 */
public class ServerSide {
	
    private static final String BASE_URI = "https://localhost:9091/";
    private static final String TRUSTSTORE_CLIENT_FILE = "config/client.truststore";
    private static final String TRUSTSTORE_CLIENT_PWD = "changeit";
    private static final String KEYSTORE_CLIENT_FILE = "config/keystore.jks";
    private static final String KEYSTORE_CLIENT_PWD = "changeit";
    private static String[] SSL_protocol; 
    private static String[] supportedCiphersuites;
    	
    static ConfigReader confReader = new ConfigReader();
    private HttpServer server = null;
	
    public ServerSide() {

        try {
	    server = startServer(false);
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
	
    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer(boolean useCliAuth) throws IOException {
        //Creates a SSLContext for the server to use TLS
        SSLContextConfigurator sslContext = new SSLContextConfigurator();

        // set up security context
        sslContext.setKeyStoreFile(KEYSTORE_CLIENT_FILE); 
        sslContext.setKeyStorePass(KEYSTORE_CLIENT_PWD);
        sslContext.setTrustStoreFile(TRUSTSTORE_CLIENT_FILE);
        sslContext.setTrustStorePass(TRUSTSTORE_CLIENT_PWD);

        try {
	    confReader.parseConfFile();
	    
        } catch (SAXException | ParserConfigurationException e) {
	    e.printStackTrace();
	}

        SSLEngineConfigurator sslEngine = new SSLEngineConfigurator(sslContext);
        
        SSL_protocol = confReader.supportedTLSv;
        supportedCiphersuites = confReader.enabledCiphersuites;
        
        sslEngine.setWantClientAuth(true);
        sslEngine.setNeedClientAuth(useCliAuth);
        sslEngine.setEnabledProtocols(SSL_protocol);
        sslEngine.setEnabledCipherSuites(supportedCiphersuites);
        sslEngine.setClientMode(false);	
        
        final ResourceConfig rc = new ResourceConfig().register(pt.unl.fct.impl.ServerSideController.class);

        final HttpServer grizzlyServer = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc, true, sslEngine, false);

        grizzlyServer.start();
        return grizzlyServer;
    }

    public void stop() {
	server.stop();
    }
   
}
