package SecureProtocol;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public class Logger {

    static final String FILE_NOT_FOUND = "The config File does not exist.";
    static final String FILE_LOCATION = "D:\\Rafael Gameiro\\Documents\\Programming\\SRSC\\TP1\\src\\SecureProtocol\\Config\\MessageLog.conf";

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
            fr = new FileReader(new File(FILE_LOCATION));
            JsonArray log = (new Gson()).fromJson(fr, JsonObject.class).getAsJsonArray(address);

            Gson gson = new Gson();
            messages = gson.fromJson(log, new TypeToken<LinkedList<String>>() {}.getType());

        } catch (FileNotFoundException e) {
            System.err.println(FILE_NOT_FOUND);
        }

    }

    protected void addMessage(String user, int sequenceNumber, String message) {
        String entry = user + "-" + sequenceNumber + "-" + message;

        messages.add(entry);
    }

    protected void storeLogs() {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter fw;
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(FILE_LOCATION));
            fw = new FileWriter(FILE_LOCATION);
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
