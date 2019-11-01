package secureProtocol;

import java.io.*;
import java.util.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.*;
import java.security.cert.CertificateException;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import exceptions.ReplayingMessageException;
import exceptions.TamperedMessageException;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;


public class SMCPSocket extends MulticastSocket {

    static final String KEYSTORE_LOCATION = "/secureProtocol/Config/SMCPKeystore.jceks";
    static final String CONFIG_LOCATION = "/secureProtocol/Config/SMCP.conf";
    static final byte VERSION_ID = 1;
    static final byte MESSAGE_TYPE = 0x01;
    static final String HASH_FUNCTION = "SHA256";
    static final String PASSWORD = "changeit";

    static final int GCM_TAG_LENGTH = 16;
    static final String[] ENCODE_MODE = {"206", "219", "257", "306", "319", "327", "506", "519", "527"};

    static final String INVALID_ADDR = "Invalid chat address";
    static final String INVALID_ALG = "Invalid Algorithm.";
    static final String CERT_EXPIRED = "Certificate Expired.";
    static final String KEYSTORE_NOT_FOUND = "KeyStore file not found.";
    static final String TAMPERED_MESSAGE = "The received message was tampered.";
    static final String REPLAYED_MESSAGE = "Replayed message.";

    private String chatID;
    private int sequenceNumb;
    private String username;
    private FileInputStream keystoreStream;
    private KeyStore keystore;
    private Logger log;
    private int offset;
    private int hashLen;
    private int macLen;
    private byte[] sessionAttr;

    private Map<String, Integer> lastMessageReceived;
    private String destUser;
    private int destSeqNumb;

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

    public SMCPSocket(int port) throws IOException {
        super(port);
        chatID = Integer.toString(port);
        sequenceNumb = 1;
        username = "";
        offset = 0;
        hashLen = 0;
        macLen = 0;

        sessionAttr = null;
        lastMessageReceived = new HashMap<>();
        destUser = "";
        destSeqNumb = 0;
    }

    @Override
    public void joinGroup(InetAddress mcastaddr) throws IOException {
        try {
            String address = mcastaddr.getHostAddress() + ":" + chatID;
            if (!chatAuthentication(address))
                throw new IOException(INVALID_ADDR);

            chatID = address;
            log = new Logger(address);
            keystoreStream = new FileInputStream((new File("").getAbsoluteFile()) + KEYSTORE_LOCATION);

            keystore = KeyStore.getInstance("JCEKS");
            keystore.load(keystoreStream, PASSWORD.toCharArray());
        } catch (NoSuchAlgorithmException e) {
            System.err.println(INVALID_ALG);
            System.exit(1);
        } catch (CertificateException e) {
            System.err.println(CERT_EXPIRED);
            System.exit(1);
        } catch (KeyStoreException e) {
            System.err.println(KEYSTORE_NOT_FOUND);
            System.exit(1);
        }
        super.joinGroup(mcastaddr);
    }

    @Override
    public void send(DatagramPacket p) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);
        Random rand = new Random();

        try {
            dataStream.writeByte(VERSION_ID);
            dataStream.writeUTF(chatID);
            dataStream.writeByte(MESSAGE_TYPE);
            dataStream.write(computeSessionAttr());

            int encode = rand.nextInt(ENCODE_MODE.length - 1);
            byte[] securePayload = computePayload(p.getData(), encode);

            if (offset == 0)
                offset = byteStream.size();
            dataStream.writeInt(encode);
            dataStream.writeInt(securePayload.length);
            dataStream.write(securePayload);
            dataStream.write(generateMAC(byteStream.toByteArray()));
            dataStream.close();

        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            System.err.println("Sending Packet error: " + e.getClass().getName() + ": "
                    + e.getMessage());
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

        super.receive(p);
        DataInputStream instream = new DataInputStream(new ByteArrayInputStream(p.getData(), p.getOffset(), p.getLength()));

        try {
            checkMessageIntegrity(p.getData());

            instream.skipBytes(offset);
            int encode = instream.readInt();

            int payloadLen = instream.readInt();
            byte[] payload = new byte[payloadLen];
            instream.read(payload, 0, payloadLen);
            payload = applyCrypto("Decrypt", encode, payload);
            payload = checkPayloadIntegrity(payload);

            messageAlreadyReceived();
            System.arraycopy(payload, 0, p.getData(), 0, payload.length);
            log.addMessage(destUser, destSeqNumb, payload);

            destUser = "";
            destSeqNumb = 0;

        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException | TamperedMessageException | ReplayingMessageException e) {
            System.err.println("Receiving Packet error: " + e.getClass().getName() + ": "
                    + e.getMessage());
        }

    }

    private void checkMessageIntegrity(byte[] data) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, InvalidKeyException, TamperedMessageException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataInputStream instream = new DataInputStream(new ByteArrayInputStream(data, 0, data.length));
        DataOutputStream outstream = new DataOutputStream(byteStream);

        byte[] mac = new byte[macLen];
        byte[] sessionAttr = new byte[this.sessionAttr.length];

        byte chatVer = instream.readByte();
        if (chatVer != VERSION_ID)
            throw new TamperedMessageException(TAMPERED_MESSAGE);
        outstream.write(chatVer);

        String chatID = instream.readUTF();
        if (!chatID.equalsIgnoreCase(this.chatID))
            throw new TamperedMessageException(TAMPERED_MESSAGE);
        outstream.writeUTF(chatID);

        byte msgtype = instream.readByte();
        if (msgtype != MESSAGE_TYPE)
            throw new TamperedMessageException(TAMPERED_MESSAGE);
        outstream.write(msgtype);

        instream.read(sessionAttr, 0, sessionAttr.length);
        outstream.write(sessionAttr);

        outstream.writeInt(instream.readInt());
        int length = instream.readInt();
        outstream.writeInt(length);

        byte[] securePayload = new byte[length];
        instream.read(securePayload, 0, length);
        outstream.write(securePayload);
        outstream.close();

        instream.read(mac, 0, macLen);
        byte[] computedMac = generateMAC(byteStream.toByteArray());
        if (!Arrays.equals(mac, computedMac))
            throw new TamperedMessageException(TAMPERED_MESSAGE);
    }

    private byte[] checkPayloadIntegrity(byte[] data) throws NoSuchAlgorithmException, IOException, TamperedMessageException {

        DataInputStream instream = new DataInputStream(new ByteArrayInputStream(data, 0, data.length));

        byte[] hash = new byte[hashLen];

        destUser = instream.readUTF();
        destSeqNumb = instream.readInt();
        instream.readInt();

        byte[] payload = new byte[data.length - destUser.length() - hashLen - 10];
        instream.read(payload, 0, payload.length);

        MessageDigest computedHash = MessageDigest.getInstance(hashName);
        computedHash.update(payload);
        instream.read(hash, 0, hashLen);

        if (!Arrays.equals(hash, computedHash.digest()))
            throw new TamperedMessageException(TAMPERED_MESSAGE);

        return payload;
    }

    private void messageAlreadyReceived() throws ReplayingMessageException {
        if (lastMessageReceived.containsKey(destUser)) {
            if (lastMessageReceived.get(destUser) < destSeqNumb)
                lastMessageReceived.replace(destUser, destSeqNumb);
            else
                throw new ReplayingMessageException(REPLAYED_MESSAGE);
        } else
            lastMessageReceived.put(destUser, destSeqNumb);
    }

    private boolean chatAuthentication(String address) {

        FileReader fr;
        try {
            fr = new FileReader(new File((new File("").getAbsoluteFile()) + CONFIG_LOCATION));

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

    private byte[] computeSessionAttr() throws NoSuchAlgorithmException, NullPointerException {

        if (sessionAttr != null)
            return sessionAttr;

        MessageDigest hash = MessageDigest.getInstance(HASH_FUNCTION);

        hash.update(chatID.getBytes());
        hash.update(sessionID.getBytes());
        hash.update(algorithmName.getBytes());
        hash.update(encryptMode.getBytes());
        hash.update(padding.getBytes());
        hash.update(hashName.getBytes());
        hash.update(macName.getBytes());

        sessionAttr = hash.digest();
        return sessionAttr;
    }

    private byte[] computePayload(byte[] payload, int encode) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);

        SecureRandom random = new SecureRandom();

        //Computes the hash of the original payload
        MessageDigest hash = MessageDigest.getInstance(hashName);
        hash.update(payload);

        if (username.equals(""))
            getUsername(payload);
        dataStream.writeUTF(username);
        dataStream.writeInt(sequenceNumb++);
        dataStream.writeInt(random.nextInt());
        dataStream.write(payload);
        dataStream.write(hash.digest());

        if (hashLen == 0)
            hashLen = hash.getDigestLength();

        dataStream.close();
        byte[] data = byteStream.toByteArray();
        data = applyCrypto("Encrypt", encode, data);

        return data;
    }

    private void getUsername(byte[] message) throws IOException {
        DataInputStream istream = new DataInputStream(new ByteArrayInputStream(message, 0, message.length));

        istream.readLong();
        istream.readInt();
        username = istream.readUTF();
    }

    private byte[] applyCrypto(String mode, int encode, byte[] data) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InvalidKeyException {

        Cipher cipher = Cipher.getInstance(algorithmName + "/" + encryptMode + "/" + padding);
        String password = getPassword("sea");
        Key key = keystore.getKey(chatID, password.toCharArray());

        byte[] ivBytes = new byte[IV.length];
        System.arraycopy(IV, 0, ivBytes, 0, IV.length);
        int step = Character.getNumericValue(ENCODE_MODE[encode].charAt(0));
        for (int i = 0; i < ivBytes.length; i += step)
            ivBytes[i] ^= ENCODE_MODE[encode].charAt(1) ^ ENCODE_MODE[encode].charAt(2);

        if (!encryptMode.equals("GCM")) {

            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            if (mode.equalsIgnoreCase("Encrypt"))
                cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            else
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

        } else {
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, ivBytes);

            if (mode.equalsIgnoreCase("Encrypt"))
                cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
            else
                cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
        }

        return cipher.doFinal(data);
    }

    private byte[] generateMAC(byte[] payload) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, InvalidKeyException {

        if (macName.equalsIgnoreCase("DES"))
            macName = macName + "CMAC";

        Mac mac = Mac.getInstance(macName);
        String password = getPassword("mac");
        Key macKey = keystore.getKey(chatID + ":MAC", password.toCharArray());
        mac.init(macKey);
        mac.update(payload);

        macLen = mac.getMacLength();
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
