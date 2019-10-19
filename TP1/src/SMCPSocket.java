import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.crypto.Mac;

import static java.security.CryptoPrimitive.MAC;

public class SMCPSocket extends MulticastSocket {

    static final byte VERSION_ID = 1;
    static final byte MESSAGE_TYPE = 0x01;
    static final String HASH_FUNCTION = "SHA256";

    static final String INVALID_ADDR = "Invalid chat address";
    static final String INVALID_HASH = "Invalid Hash function.";

    private String chatID;
    private int sequenceNumb;

    //Obtained from JSON
    private String sessionID;
    private String algorithmName;
    private int keySize;
    private String encryptMode;
    private String padding;
    private String hashName;
    private String macName;
    private int macKeySize;

    public SMCPSocket(int port) throws IOException {
        super(port);
        chatID = Integer.toString(port);
        sequenceNumb = 1;
    }

    @Override
    public void joinGroup(InetAddress mcastaddr) throws IOException {
        String address = mcastaddr.getHostAddress() + ":" + chatID;
        if(!chatAuthentication(address))
            throw new IOException(INVALID_ADDR);

        chatID = address;
        super.joinGroup(mcastaddr);
    }

    @Override
    public void send(DatagramPacket p) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);

        try {

            dataStream.writeByte(VERSION_ID);
            dataStream.writeUTF(chatID);
            dataStream.writeByte(MESSAGE_TYPE);
            dataStream.write(computeSessionAttr());
            byte [] securePayload = computePayload(p.getData());

            dataStream.writeInt(securePayload.length);
            dataStream.write(securePayload);
            dataStream.write(generateMAC(byteStream.toByteArray()));
            dataStream.close();

        }catch (NoSuchAlgorithmException e) {
            System.err.println(INVALID_HASH);
        }

        byte[] data = byteStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, p.getAddress(), p.getPort());

        super.send(packet);
    }

    @Override
    public void receive(DatagramPacket p) throws IOException {
        super.receive(p);

        DataInputStream istream = new DataInputStream(new ByteArrayInputStream(p.getData(), p.getOffset(), p.getLength()));

        long magic = istream.readLong();
        int opCode = istream.readInt();
        String name = istream.readUTF();
        System.out.println(magic);
        System.out.println(opCode);
        System.out.println(name);
        if(opCode == 3) {
            String message = istream.readUTF();
            System.out.println(message);
        }
    }

    private boolean chatAuthentication(String address) {
        FileReader fr = null;

        try {
            //USE FOR WINDOWS!!!!
            fr = new FileReader(new File("D:\\Rafael Gameiro\\Documents\\Programming\\SRSC\\TP1\\src\\SMCP.conf"));
            //USE FOR LINUX!!!
            //fr = new FileReader(new File("./SMCP.conf"));

            JsonObject endpoint = (new Gson()).fromJson(fr, JsonObject.class).getAsJsonObject(address);
            if (endpoint == null)
                return false;

            sessionID = endpoint.get("SID").getAsString();
            algorithmName = endpoint.get("SEA").getAsString();
            keySize = endpoint.get("SEAKS").getAsInt();
            encryptMode = endpoint.get("Mode").getAsString();
            padding = endpoint.get("Padding").getAsString();
            hashName = endpoint.get("IntHash").getAsString();
            macName = endpoint.get("MAC").getAsString();
            macKeySize = endpoint.get("MAKKS").getAsInt();

        } catch (FileNotFoundException e) {
            return false;
        }

        return true;
    }

    private byte[] computeSessionAttr() throws NoSuchAlgorithmException {

        MessageDigest hash = MessageDigest.getInstance(HASH_FUNCTION);

        hash.update(chatID.getBytes());
        hash.update(sessionID.getBytes());
        hash.update(algorithmName.getBytes());
        hash.update(encryptMode.getBytes());
        hash.update(padding.getBytes());
        hash.update(hashName.getBytes());
        hash.update(macName.getBytes());

        return hash.digest();
    }

    private byte[] computePayload(byte[] payload) throws NoSuchAlgorithmException, IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);

        //Computes the hash of the original payload
        MessageDigest hash = MessageDigest.getInstance(hashName);
        hash.update(payload);

        //TODO get userID or user nickname
        dataStream.write(1);
        dataStream.write(sequenceNumb++);
        //TODO check how to create an psudo random number
        dataStream.write(Utils.createFixedRandom());
        dataStream.write(payload);
        dataStream.write(hash.digest());

        dataStream.close();
        byte[] data = byteStream.toByteArray();

        //TODO Apply cryptography

        return data;
    }

    private byte[] generateMAC(byte[] payload) {

        Mac mac = MAC.getInstance(macName);
        //fazer mac.init(key)

        mac.update(payload);

        return mac.doFinal();
    }

}