package pt.unl.fct.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Rafael
 * @author Manuella
 */

/**
 * Class reponsible for the processing of the TLS configuration file
 */
public class ConfigReader {
	private static final String CONF_FILEPATH = "config/TLS.conf";
	public String[] enabledCiphersuites;
	public String[] supportedTLSv;
	public String[] authenticationMode; 
	
		public void parseConfFile() throws SAXException, IOException, ParserConfigurationException {
			
			File fXmlFile = new File(CONF_FILEPATH);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
			Document confFile = (Document) docBuilder.parse(fXmlFile);
			confFile.getDocumentElement().normalize();
			
			Element root = confFile.getDocumentElement();
			
			Node tls_version = confFile.getElementsByTagName("TLSVERSION").item(0);
			Node ciphersuites = confFile.getElementsByTagName("ENABLEDCIPHERSUITES").item(0);
			Node auth_mode = confFile.getElementsByTagName("AUTHMODE").item(0);
			
			String tls_v = tls_version.getTextContent().replaceAll("\n", "");
			String cipher = ciphersuites.getTextContent().replaceAll("\n", "");
			String auth_m = auth_mode.getTextContent().replaceAll("\n", "");
			
			enabledCiphersuites = cipher.split(";");
			supportedTLSv = tls_v.split(";");
			authenticationMode = auth_m.split(";");
		}	
}
