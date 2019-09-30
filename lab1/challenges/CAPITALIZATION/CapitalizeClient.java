import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A simple Swing-based client for the capitalization server.
 * It has a main frame window with a text field for entering
 * strings and a textarea to see the results of capitalizing
 * them.
 */
public class CapitalizeClient {

    private static final String ALGORITHM = "AES";
    private static final String INSTANCE = "AES/CBC/PKCS5Padding"; 
    private static final String KEY = "B5F97243578AC94923211EA5E752B8EE";
    private static final String IV = "olasdfredscsafdg";

    private BufferedReader in;
    private PrintWriter out;
    private JFrame frame = new JFrame("Capitalize Client");
    private JTextField dataField = new JTextField(40);
    private JTextArea messageArea = new JTextArea(8, 60);

    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Enter in the
     * listener sends the textfield contents to the server.
     */
    public CapitalizeClient() {

        // Layout GUI
        messageArea.setEditable(false);
        frame.getContentPane().add(dataField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        
        try {

            //initialize the cypher the program will use to encrypt the data
            SecretKeySpec key = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());
            Cipher c = Cipher.getInstance(INSTANCE);

            // Add Listeners
            dataField.addActionListener(new ActionListener() {
	    	    /**
		    * Responds to pressing the enter key in the textfield
		    * by sending the contents of the text field to the
		    * server and displaying the response from the server
		    * in the text area.  If the response is "." we exit
		    * the whole application, which closes all sockets,
		    * streams and windows.
		    */
		    public void actionPerformed(ActionEvent e) {
                        String text = dataField.getText();
                        String response;
                        try {
                            c.init(Cipher.ENCRYPT_MODE, key, ivSpec);
                            byte[] encrypted = c.doFinal(text.getBytes());

                            out.println(Base64.getEncoder().encodeToString(encrypted));

                            c.init(Cipher.DECRYPT_MODE, key, ivSpec);
			    response = in.readLine();

			    if (response == null || response.equals("")) {
			        System.exit(0);
			    }
                            response =
                             new String(c.doFinal(Base64.getDecoder().decode(response)));

		        } catch (Exception ex) {
			    response = "Error: " + ex;
		        }
                    

		        messageArea.append(response + "\n");
		        dataField.selectAll();
		    }
	        });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Implements the connection logic by prompting the end user for
     * the server's IP address, connecting, setting up streams, and
     * consuming the welcome messages from the server.  The Capitalizer
     * protocol says that the server sends three lines of text to the
     * client immediately after establishing a connection.
     */
    public void connectToServer() throws IOException {

        // Get the server address from a dialog box.
        String serverAddress = JOptionPane.showInputDialog(
							   frame,
							   "Enter IP Address of the Server:",
							   "Welcome to the Capitalization Program",
							   JOptionPane.QUESTION_MESSAGE);

        // Make connection and initialize streams
        Socket socket = new Socket(serverAddress, 9898);
        in = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        
        // Consume the initial welcoming messages from the server
        for (int i = 0; i < 3; i++) {
            messageArea.append(in.readLine() + "\n");
        }
    }

    /**
     * Runs the client application.
     */
    public static void main(String[] args) throws Exception {
        CapitalizeClient client = new CapitalizeClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.pack();
        client.frame.setVisible(true);
        client.connectToServer();
    }
}
