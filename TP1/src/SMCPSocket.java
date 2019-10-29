import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SMCPSocket extends MulticastSocket {

    static final byte VERSION_ID = 1;
    static final byte MESSAGE_TYPE = 0x01;
    static final String HASH_FUNCTION = "SHA256";
    static final String PASSWORD = "changeit";

    static final int GCM_TAG_LENGTH = 16;
    static final String[] ENCODE_MODE = {"206", "219", "257", "306", "319", "327", "506", "519", "527"};

    static final String INVALID_ADDR = "Invalid chat address";
    static final String INVALID_HASH = "Invalid Hash function.";
    static final String INVALID_ALG = "Invalid Algorithm.";
    static final String CERT_EXPIRED = "Certificate Expired.";

    private String chatID;
    private int sequenceNumb;
    private String username;
    private FileInputStream keystoreStream;
    private KeyStore keystore;
    private int offset;
    private int messageOff;
    private int currMsgLen;
    private int packetLen;
    private int macLen;

    //Obtained from JSON
    private String sessionID;
    private String algorithmName;
    private int keySize;
    private String encryptMode;
    private String padding;
    private byte[] IV;
    private String hashName;
    private String macName;
    private int macKeySize;


    //TODO change later!!!
    private byte[] ivBytes;

    public SMCPSocket(int port) throws IOException, KeyStoreException {
        super(port);
        chatID = Integer.toString(port);
        sequenceNumb = 1;
        keystore = KeyStore.getInstance("JCEKS");
        username = "";
        offset = 0;
    }

    @Override
    public void joinGroup(InetAddress mcastaddr) throws IOException {
        try {
            String address = mcastaddr.getHostAddress() + ":" + chatID;
            if (!chatAuthentication(address))
                throw new IOException(INVALID_ADDR);

            chatID = address;
            //USE FOR WINDOWS!!!!
            //keystoreStream = new FileInputStream("D:\\Rafael Gameiro\\Documents\\Programming\\SRSC\\TP1\\src\\SMCPKeystore.jceks");
            //USE FOR LINUX!!!
            keystoreStream = new FileInputStream("/home/arch/Documents/Programming/Java/SRSC/TP1/src/SMCPKeystore.jceks");

            keystore.load(keystoreStream, PASSWORD.toCharArray());
        } catch (NoSuchAlgorithmException e) {
            System.err.println(INVALID_ALG);
        } catch (CertificateException e) {
            System.err.println(CERT_EXPIRED);
        }

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
            byte[] securePayload = computePayload(p.getData());

            offset = byteStream.size();
            dataStream.writeInt(securePayload.length);
            dataStream.write(securePayload);
            dataStream.write(generateMAC(byteStream.toByteArray()));
            dataStream.close();

        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            System.err.println(INVALID_HASH);
            e.printStackTrace();
        }

        byte[] data = byteStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, p.getAddress(), p.getPort());

        super.send(packet);
    }

    @Override
    public void receive(DatagramPacket p) throws IOException {

        //checks if the message hash is the same as the integrity hash
        //if it is, gets the secure payload
        //does the cryptography
        //checks if the payload hash is the same as the previously computed
        //if it is, returns the message
        //calls super.receive(message)
        DataInputStream instream = new DataInputStream(new ByteArrayInputStream(p.getData(), p.getOffset(), p.getLength()));

        try {
            if (!checkMessageIntegrity(p.getData()))
                System.err.println("The received message was corrupted!");

            instream.skipBytes(offset);
            int payloadLen = instream.readInt();
            byte[] payload = new byte[payloadLen];
            instream.read(payload, 0, payloadLen);
            payload = applyCrypto("Decrypt", payload);

            payload = checkPayloadIntegrity(payload);
            if (payload.equals(null))
                System.err.println("The received message was corrupted!");

            p = new DatagramPacket(payload, currMsgLen, p.getAddress(), p.getPort());

        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException |
                InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException |
                BadPaddingException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        super.receive(p);
    }

    private boolean checkMessageIntegrity(byte[] data) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, InvalidKeyException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataInputStream instream = new DataInputStream(new ByteArrayInputStream(data, 0, data.length));
        DataOutputStream outStream = new DataOutputStream(byteStream);

        byte[] mac = new byte[macLen];

        byte[] buffer = new byte[packetLen];
        instream.read(buffer, 0, packetLen);
        outStream.write(buffer, 0, packetLen);

        instream.readFully(mac);
        byte[] computedMac = generateMAC(byteStream.toByteArray());

        return Arrays.equals(mac, computedMac);
    }

    private byte[] checkPayloadIntegrity(byte[] data) throws NoSuchAlgorithmException, IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataInputStream instream = new DataInputStream(new ByteArrayInputStream(data, 0, data.length));
        DataOutputStream outStream = new DataOutputStream(byteStream);

        byte[] hash = new byte[macLen];
        byte[] payload = new byte[currMsgLen];

        //read everything to inside the instream
        instream.skipBytes(messageOff);
        instream.read(payload, 0, currMsgLen);

        MessageDigest computedHash = MessageDigest.getInstance(hashName);
        computedHash.update(payload);
        instream.readFully(hash);

        return Arrays.equals(hash, computedHash.digest()) ? payload : null;
    }

    private boolean chatAuthentication(String address) {
        FileReader fr = null;

        try {
            //USE FOR WINDOWS!!!!
            fr = new FileReader(new File("D:\\Rafael Gameiro\\Documents\\Programming\\SRSC\\TP1\\src\\SMCP.conf"));
            //USE FOR LINUX!!!
            fr = new FileReader(new File("/home/arch/Documents/Programming/Java/SRSC/TP1/src/SMCP.conf"));

            JsonObject endpoint = (new Gson()).fromJson(fr, JsonObject.class).getAsJsonObject(address);
            if (endpoint == null)
                return false;

            sessionID = endpoint.get("SID").getAsString();
            algorithmName = endpoint.get("SEA").getAsString();
            keySize = endpoint.get("SEAKS").getAsInt();
            encryptMode = endpoint.get("Mode").getAsString();
            padding = endpoint.get("Padding").getAsString();
            IV = Base64.getDecoder().decode(endpoint.get("IV").getAsString().getBytes());
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

        byte[] digest = hash.digest();
        offset += digest.length;

        return digest;
    }

    private byte[] computePayload(byte[] payload) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);

        SecureRandom random = new SecureRandom();
        currMsgLen = payload.length;

        //Computes the hash of the original payload
        MessageDigest hash = MessageDigest.getInstance(hashName);
        hash.update(payload);

        if (username.equals(""))
            getUsername(payload);
        dataStream.write(username.getBytes());
        dataStream.write(sequenceNumb++);
        dataStream.write(random.nextInt());
        messageOff = byteStream.size();
        dataStream.write(payload);
        dataStream.write(hash.digest());

        dataStream.close();
        byte[] data = byteStream.toByteArray();

        data = applyCrypto("Encrypt", data);

        return data;
    }

    private void getUsername(byte[] message) throws IOException {
        DataInputStream istream = new DataInputStream(new ByteArrayInputStream(message, 0, message.length));

        istream.readLong();
        istream.readInt();
        username = istream.readUTF();
    }

    private byte[] applyCrypto(String mode, byte[] data) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InvalidKeyException {

        Cipher cipher = Cipher.getInstance(algorithmName + "/" + encryptMode + "/" + padding);
        String password = getPassword("sea");
        Key key = keystore.getKey(chatID, password.toCharArray());

        if(ivBytes == null)
            ivBytes = SecureRandom.getSeed(12);

        if (!encryptMode.equals("GCM")) {

            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            if (mode.equalsIgnoreCase("Encrypt"))
                cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            else
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

        } else {
            System.out.println("Entrou aqui ne??");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, ivBytes);

            if (mode.equalsIgnoreCase("Encrypt"))
                cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
            else
                cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
        }

        return cipher.doFinal(data);
    }

    private byte[] generateMAC(byte[] payload) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, InvalidKeyException {

        packetLen = payload.length;
        Mac mac = Mac.getInstance(macName);
        String password = getPassword("mac");
        Key macKey = keystore.getKey(chatID + ":MAC", password.toCharArray());
        mac.init(macKey);
        macLen = mac.getMacLength();

        mac.update(payload);

        mac.getMacLength();

        return mac.doFinal();
    }

    private String getPassword(String type) throws NoSuchAlgorithmException {

        MessageDigest hash = MessageDigest.getInstance(hashName);

        String[] chatName = sessionID.split(" ");

        for (int i = 0; i < chatName.length; i++) {
            String word = Character.toString(chatName[i].charAt(0)).toUpperCase() + chatName[i].substring(1);
            hash.update(word.getBytes());
        }

        hash.update(type.toUpperCase().getBytes());

        return Base64.getEncoder().encodeToString(hash.digest()).substring(0, 16);

    }

}
