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

import java.util.LinkedList;
import java.util.List;

public class MyResourceTest {

    private HttpServer server;
    private WebTarget target;
}
