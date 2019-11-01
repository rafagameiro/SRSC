package SecureProtocol;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public class Logger {

    static final String FILE_NOT_FOUND = "The config File does not exist.";
    static final String FILE_LOCATION = "/SecureProtocol/Config/MessageLog.conf";

    static final String CHAT_JOIN_MESSAGE ="NOVO PARTICIPANTE: %s juntou-se ao grupo do chat.";
    static final String CHAT_LEAVE_MESSAGE ="ABANDONO: %s abandonou o grupo de chat.";

    static final int JOIN = 1;
    static final int LEAVE = 2;

    private List<String> messages;
    private String address;

    public Logger(String address) {
        messages = new LinkedList<>();
        this.address = address;
        getLogs();
    }

    private void getLogs() {
        FileReader fr;
        try {
            fr = new FileReader(new File((new File("").getAbsoluteFile()) + FILE_LOCATION));
            JsonArray log = (new Gson()).fromJson(fr, JsonObject.class).getAsJsonArray(address);

            Gson gson = new Gson();
            messages = gson.fromJson(log, new TypeToken<LinkedList<String>>() {}.getType());

        } catch (FileNotFoundException e) {
            System.err.println(FILE_NOT_FOUND);
        }

    }

    protected void addMessage(String user, int sequenceNumber, byte[] payload) throws IOException {

        int code = 0;
        String message = "";
        try {
            DataInputStream instream = new DataInputStream(new ByteArrayInputStream(payload, 0, payload.length));
            instream.readLong();
            code = instream.readInt();
            instream.readUTF();

            message = instream.readUTF();
        }catch (EOFException e) {
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
            e.printStackTrace();
        }
    }
}
