package pt.unl.fct.impl;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.net.ssl.SSLContext;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;

 
public class ClientController {

    private static final String KEYSTORE_SERVER_FILE = "config/keystore.jks";
    private static final String KEYSTORE_SERVER_PWD = "changeit";
    private static final String TRUSTSTORE_SERVER_FILE = "config/server.truststore"; 
    private static final String TRUSTSTORE_SERVER_PWD = "changeit";

    private Client client;

    public ClientController() {
    	client = setClient();
    }

    public Response updateAuction(String clientURI, String auctionId, String message) {
        return client.target(clientURI + "/client").path("update-auction/" + auctionId).request().post(Entity.entity(message, MediaType.APPLICATION_JSON));
    }

    private Client setClient() {
		
        SslConfigurator sslConfig = SslConfigurator.newInstance()
                .trustStoreFile(TRUSTSTORE_SERVER_FILE)
                .trustStorePassword(TRUSTSTORE_SERVER_PWD)
                .keyStoreFile(KEYSTORE_SERVER_FILE)
                .keyPassword(KEYSTORE_SERVER_PWD);
        
        final ClientConfig clientConfig = new ClientConfig().connectorProvider(new HttpUrlConnectorProvider());
        final SSLContext sslContext = sslConfig.createSSLContext();
        Client client = ClientBuilder.newBuilder().withConfig(clientConfig).sslContext(sslContext).build();
        return client; 
    }
	
}
