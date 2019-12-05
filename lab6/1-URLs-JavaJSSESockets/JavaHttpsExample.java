import java.net.URL;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;

public class JavaHttpsExample
{
   
    public static void main(String[] args)
       throws Exception
    {

	// Ex., String httpsURL = "https://www.bpinet.pt:443";
	String httpsURL = args[0];
	URL myurl = new URL(httpsURL);
	HttpsURLConnection con = (HttpsURLConnection)myurl.openConnection();
	InputStream ins = con.getInputStream();
	InputStreamReader isr=new InputStreamReader(ins);
	BufferedReader in =new BufferedReader(isr);
	
	String inputLine;
	
	while ((inputLine = in.readLine()) != null)
	    System.out.println(inputLine);
	
	in.close();
	
    }
   
}