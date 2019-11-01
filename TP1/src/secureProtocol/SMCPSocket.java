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
    /**
     * Checks if the typed address and port match some chat entry
     * If it does, loads a log stored in memory will all the previous messages
     * And it also creates a stream to the keystore in order to access the keys needed to use the algorithm
     * associated with the current chat room
     */
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
    /**
     * Creates a new packet to be sent to the users in the chat room
     * The methods goes through the following process
     * Inserts inside a byte stream the version of the protocol, the chat identifier, the message type code and
     * the attributes associated with the chat room, needed to perform the security
     * After that, encrypts the payload using symmetric cryptography and puts inside the byte stream
     * To do that, the program will generate a random number between 0 and the length of the constant string array ENCODE_MODE
     * With that, the program will generate an IV which will be used to encrypt the payload
     * The position of the array used to generate the IV is put in the byte stream,
     * as the length of the new payload and payload itself
     * Finally, a MAC is generated in order to provide some integrity prove and with the data for the datagram packet is generated
     * The method super.send() is called and the packet is send to the other users in the chat, including the local user.
     */
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
    /**
     * calls super.receive(message) to receive the message
     * checks if the message computed hash is the same as the integrity hash
     * if it is, gets the secure payload by decrypting it
     * checks if the payload hash is the same as the previously computed
     * After that, verifies if the received message is a duplicated ou a replying attack
     * If it is, the message will not be processed, but if it is not, it will copy the information
     * into the buffer inside the datagram packet
     */
    public void receive(DatagramPacket p) throws IOException {

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

    /**
     * Checks if the received packet as tampered
     * It places all the information in the packet except the mac, inside a byte stream
     * With that, computes the respective mac and compares the two of them
     * If both are the same, the message was not tampered
     * The method also checks if the other parameters inside the packet have the values they are supposed to have
     * If some parameter has a different value, the program assumes it was changed through a tampered attack
     *
     * @param data data contained inside the packet
     * @throws IOException               If some error happens with the creation of the streams
     * @throws UnrecoverableKeyException If the key in the keystore can no longer be retrieved
     * @throws NoSuchAlgorithmException  If the algorithm name used to generate the MAC does not exist
     * @throws KeyStoreException         If the keystore does not have an entry with such alias
     * @throws InvalidKeyException       If the inserted key does not have a specific size or belongs to a different algorithm
     * @throws TamperedMessageException  If the message suffered a tampered attack
     */
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

    /**
     * Checks if the received payload is the same as the one created by the sender
     * The verification is made by generating an hash will the plaintext and
     * comparing it with the hash the sender computed during the send process
     *
     * @param data data corresponding to the payload
     * @return the original payload without cryptography and extra parameters
     * @throws NoSuchAlgorithmException If the algorithm name used to generate the hash does not exist
     * @throws IOException              If some error happens with the creation of the streams
     * @throws TamperedMessageException If the payload was tampered
     */
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

    /**
     * Checks in the hash map if the sender's entry has a value equal or superior to the one in the current message
     * If the user is not registed yet, in the map, is created an entry with the value being the sequence number
     * inside the message
     * If the sender already has an entry in the map, checks if the sequence number in the current message
     * is equal or inferior to the one registered in the map, and if it is throws a ReplayingMessageException
     *
     * @throws ReplayingMessageException if the current processed message originated from a replaying attack
     */
    private void messageAlreadyReceived() throws ReplayingMessageException {
        if (lastMessageReceived.containsKey(destUser)) {
            if (lastMessageReceived.get(destUser) < destSeqNumb)
                lastMessageReceived.replace(destUser, destSeqNumb);
            else
                throw new ReplayingMessageException(REPLAYED_MESSAGE);
        } else
            lastMessageReceived.put(destUser, destSeqNumb);
    }

    /**
     * Accesses the chat configuration settings in the config file in the Config folder
     * If the the file does not exist or was not correctly accessed, throws a FileNotFoundException
     *
     * @param address address typed by the user in order to access the chat
     * @return true if the parameters were obtained, false otherwise
     */
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

    /**
     * Generates an hash with attributes related to current chat's encryption
     *
     * @return the hash computed in the MAC
     * @throws NoSuchAlgorithmException If the algorithm name used to generate the hash does not exist
     */
    private byte[] computeSessionAttr() throws NoSuchAlgorithmException {

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

    /**
     * Computes the secure payload of the packet to send
     * The method will store in a byte stream, the username, the sequence number of the message, a nonce, the plaintext
     * and the hash, computed with the plain text
     * After inserting all those elements in the stream, the byte array that has the elements will be encrypted in order to
     * protect the packet against possible attackers
     *
     * @param payload original plaintext
     * @param encode  integer that corresponds to a position in the array ENCODE_MODE
     * @return the new payload, with more fields and encrypted
     * @throws UnrecoverableKeyException          If the key in the keystore can no longer be retrieved
     * @throws NoSuchAlgorithmException           If the algorithm name used to do the symmetric cryptography does not exist
     * @throws KeyStoreException                  If the keystore does not have an entry with such alias
     * @throws NoSuchPaddingException             If the program did no specify the padding to do the cryptography
     * @throws BadPaddingException                If the program inserted a padding that does not exist for that algorithm instance
     * @throws IllegalBlockSizeException          If the inserted block size is too long or too short for the specified algorithm
     * @throws InvalidAlgorithmParameterException If the parameters inserted are invalid for the specified algorithm
     * @throws InvalidKeyException                If the inserted key does not have a specific size or belongs to a different algorithm
     * @throws IOException                        If some error happens with the creation of the streams
     */
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

    /**
     * Gets the the nickname of the local user and stores it
     *
     * @param message message to be processed
     * @throws IOException If some error happens with the creation of the DataInputStream
     */
    private void getUsername(byte[] message) throws IOException {
        DataInputStream istream = new DataInputStream(new ByteArrayInputStream(message, 0, message.length));

        istream.readLong();
        istream.readInt();
        username = istream.readUTF();
    }

    /**
     * Encrypts the new generated payload using symmetric cryptography
     * Firstly, it will access the keystore the retrieve the key needed to perform the cryptography
     * After that it will generate the IV used to start the cryptography
     *
     * The generation of the IV e somewhat complicated
     * All the users that are in the chat room have a default IV, but in order to give some security
     * its bytes are tampered in order to generate new IV from the original IV
     *
     * With that, the program will perform the cryptography
     * If the algorithm used to encrypt/decrypt is GCM, a different ParameterSpec object will be generated
     *
     * @param mode operation the method will executing. It can encrypt or decrypt
     * @param encode integer that corresponds to a position in the array ENCODE_MODE
     * @param data new payload with the plaintext and other additional fields
     * @return the data encrypted
     * @throws UnrecoverableKeyException If the key in the keystore can no longer be retrieved
     * @throws NoSuchAlgorithmException If the algorithm name used to do the symmetric cryptography does not exist
     * @throws KeyStoreException If the keystore does not have an entry with such alias
     * @throws NoSuchPaddingException If the program did no specify the padding to do the cryptography
     * @throws BadPaddingException If the program inserted a padding that does not exist for that algorithm instance
     * @throws IllegalBlockSizeException If the inserted block size is too long or too short for the specified algorithm
     * @throws InvalidAlgorithmParameterException If the parameters inserted are invalid for the specified algorithm
     * @throws InvalidKeyException If the inserted key does not have a specific size or belongs to a different algorithm
     */
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

    /**
     * Computes the MAC of the payload, in order to provide a security check
     *
     * @param payload array of bytes that will be used to computed the MAC
     * @return the hash computed in the MAC
     * @throws UnrecoverableKeyException If the key in the keystore can no longer be retrieved
     * @throws NoSuchAlgorithmException  If the algorithm name used to generate the MAC does not exist
     * @throws KeyStoreException         If the keystore does not have an entry with such alias
     * @throws InvalidKeyException       If the inserted key does not have a specific size or belongs to a different algorithm
     */
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

    /**
     * Generates the password associated with a keystore entry
     * Computes the hash of the title of the chat and add the type of keystore entry wanted
     * It might me a symmetric algorithm or a MAC
     * After computing the hash, cast its value into string and returns a substring which is the keystore entry's password
     *
     * @param type type of keystore entry
     * @return password of keystore entry
     * @throws NoSuchAlgorithmException if the algorithm used to computed the hash does not exist
     */
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
