import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

public class JavaHttpsExampleCertificate
{
     public static void main(String[] args)
     {
       new JavaHttpsExampleCertificate().checkCertificate();
     }
   
     private void checkCertificate()
     {
      // Put what you want ...
      String https_url = "https://www.google.com/";
      URL url;
      try 
        {
          url = new URL(https_url);
          HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
          //dumpl the cert info
          print_https_cert(con);
          //dump the content
          print_content(con);
         }
         catch (MalformedURLException e) 
           {
           e.printStackTrace();
	   }
         catch (IOException e) 
	   {
	   e.printStackTrace();
	   }
     }
   
     private void print_https_cert(HttpsURLConnection con)
     {
	 if(con!=null)
	 {
         try 
            {
            System.out.println("Response Code : " + con.getResponseCode());
	    System.out.println("Cipher Suite : " + con.getCipherSuite());
	    // See also other HttpsURLCOnnection methods   
	    System.out.println("\n");
		  
	    Certificate[] certs = con.getServerCertificates();
	    for(Certificate cert : certs)
	    {
	       System.out.println("Cert Type : " + cert.getType());
	       System.out.println("Cert Hash Code : " + cert.hashCode());
	       
    	       System.out.println("Representation:");
	       System.out.println(cert.toString());
               System.out.println("Cert Public Key Algorithm : ");
               System.out.println("Cert Public Key Format : ");

	    }
	       
	       
	   }
	   catch (SSLPeerUnverifiedException e) 
	    {
             e.printStackTrace();
	    }
	   catch (IOException e)
	    {
             e.printStackTrace();
            }
	 }
      }

     private void print_content(HttpsURLConnection con)
     {
	if(con!=null)
	  {
	     try 
	       {
		  System.out.println("****** Content of the URL ********");
            	  BufferedReader br = 
		  new BufferedReader(
		       new InputStreamReader(con.getInputStream()));
		  
		  String input;
                  while ((input = br.readLine()) != null)
		    {
	             System.out.println(input);
		    }
		     br.close();
	       }
	      catch (IOException e) 
	       {
		     e.printStackTrace();
	       }
	  }
     }
}
