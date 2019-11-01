package secureProtocol;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public class Logger {

    static final String FILE_NOT_FOUND = "The config File does not exist.";
    static final String IO_EXCEPTION = "An error occurred during the creation o the writing of the log.";
    static final String FILE_LOCATION = "/secureProtocol/Config/MessageLog.conf";

    static final String CHAT_JOIN_MESSAGE = "NOVO PARTICIPANTE: %s juntou-se ao grupo do chat.";
    static final String CHAT_LEAVE_MESSAGE = "ABANDONO: %s abandonou o grupo de chat.";

    static final int JOIN = 1;
    static final int LEAVE = 2;

    private List<String> messages;
    private String address;

    public Logger(String address) {
        messages = new LinkedList<>();
        this.address = address;
        getLogs();
    }

    /**
     * Retrieves the log from the last access to the chat room
     * If the file containing the logs was not found or correctly open an FileNotFoundException is thrown
     */
    private void getLogs() {
        FileReader fr;
        try {
            fr = new FileReader(new File((new File("").getAbsoluteFile()) + FILE_LOCATION));
            JsonArray log = (new Gson()).fromJson(fr, JsonObject.class).getAsJsonArray(address);

            Gson gson = new Gson();
            messages = gson.fromJson(log, new TypeToken<LinkedList<String>>() {
            }.getType());

        } catch (FileNotFoundException e) {
            System.err.println(FILE_NOT_FOUND);
        }

    }

    /**
     * Adds the message into the messages list
     * If it is a join message or leave message, a default message template is used to generate the entry
     * If it is a leave message, the program will store the messages array in a file in order to preserve the registry
     *
     * @param user           sender of the message
     * @param sequenceNumber sequence number the current message has
     * @param payload        plaintext of the received message
     * @throws IOException If some error happens with the creation of the DataInputStream
     */
    protected void addMessage(String user, int sequenceNumber, byte[] payload) throws IOException {

        int code = 0;
        String message = "";
        try {
            DataInputStream instream = new DataInputStream(new ByteArrayInputStream(payload, 0, payload.length));
            instream.readLong();
            code = instream.readInt();
            instream.readUTF();

            message = instream.readUTF();
        } catch (EOFException e) {
            switch (code) {
                case JOIN:
                    message = String.format(CHAT_JOIN_MESSAGE, user);
                    break;
                case LEAVE:
                    message = String.format(CHAT_LEAVE_MESSAGE, user);
                    break;
            }
        }

        String entry = user + "-" + sequenceNumber + "-" + message;
        messages.add(entry);

        if (code == 2)
            storeLogs();
    }

    /**
     * Stores the messages list into the config file in order to preserve the registry
     * If the file was not found an FileNotFoundException is thrown
     * If some error happens during the writing in the file, an IOException is thrown
     */
    private void storeLogs() {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter fw;
        BufferedReader br;
        try {

            br = new BufferedReader(new FileReader(new File((new File("").getAbsoluteFile()) + FILE_LOCATION)));
            fw = new FileWriter(new File((new File("").getAbsoluteFile()) + FILE_LOCATION));
            JsonObject log = (new Gson()).fromJson(br, JsonObject.class).getAsJsonObject();
            JsonElement element = gson.toJsonTree(messages, new TypeToken<LinkedList<String>>() {
            }.getType());

            log.add(address, element.getAsJsonArray());
            gson.toJson(log, fw);

            fw.flush();
            fw.close();

        } catch (FileNotFoundException e) {
            System.err.println(FILE_NOT_FOUND);
        } catch (IOException e) {
            System.err.println(IO_EXCEPTION);
        }
    }
}
