import java.util.*;
import java.io.*;
import java.net.*;
import javax.net.ssl.*;

public class TLSClient {

    public static void main(String[] args) {
	BufferedReader in = new BufferedReader(
		       new InputStreamReader(System.in));
	PrintStream out = System.out;

        SSLSocketFactory f = 
	  (SSLSocketFactory) SSLSocketFactory.getDefault();
        try {
         SSLSocket c =
	     (SSLSocket) f.createSocket(args[0], Integer.parseInt(args[1]));

         c.startHandshake();
         BufferedWriter w = new BufferedWriter(
	       new OutputStreamWriter(c.getOutputStream()));
         BufferedReader r = new BufferedReader(
	       new InputStreamReader(c.getInputStream()));
         String m = null;
         while ((m=r.readLine())!= "!quit") {
	     out.println(m);
	     m = in.readLine();
             System.out.println("input:"+ m);

	     w.write(m,0,m.length());
	     w.newLine();
	     w.flush();
         }
         w.close();
         r.close();
         c.close();
      } catch (IOException e) {
	  System.err.println(e.toString());
      }
    }
}
