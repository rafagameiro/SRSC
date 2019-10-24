import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.*;
import java.security.cert.CertificateException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

public class SMCPSocket extends MulticastSocket {

    static final byte VERSION_ID = 1;
    static final byte MESSAGE_TYPE = 0x01;
    static final String HASH_FUNCTION = "SHA256";
    static final String PASSWORD = "changeit";

    static final int GCM_TAG_LENGTH = 16;

    static final String INVALID_ADDR = "Invalid chat address";
    static final String INVALID_HASH = "Invalid Hash function.";
    static final String INVALID_ALG = "Invalid Algorithm.";
    static final String CERT_EXPIRED = "Certificate Expired.";

    private String chatID;
    private int sequenceNumb;
    private String username;
    private FileInputStream keystoreStream;
    private KeyStore keystore;

    //Obtained from JSON
    private String sessionID;
    private String algorithmName;
    private int keySize;
    private String encryptMode;
    private String padding;
    private String hashName;
    private String macName;
    private int macKeySize;

    public SMCPSocket(int port) throws IOException, KeyStoreException {
        super(port);
        chatID = Integer.toString(port);
        sequenceNumb = 1;
        keystore = KeyStore.getInstance("JCEKS");
        username = "";
    }

    @Override
    public void joinGroup(InetAddress mcastaddr) throws IOException {
        try {
            String address = mcastaddr.getHostAddress() + ":" + chatID;
            if (!chatAuthentication(address))
                throw new IOException(INVALID_ADDR);

            chatID = address;
            keystoreStream = new FileInputStream("SMCPKeystore.jceks");

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

            dataStream.writeInt(securePayload.length);
            dataStream.write(securePayload);
            dataStream.write(generateMAC(byteStream.toByteArray()));
            dataStream.close();

        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException | InvalidKeyException e) {
            System.err.println(INVALID_HASH);
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

        DataInputStream istream = new DataInputStream(new ByteArrayInputStream(p.getData(), p.getOffset(), p.getLength()));

        long magic = istream.readLong();
        int opCode = istream.readInt();
        String name = istream.readUTF();
        System.out.println(magic);
        System.out.println(opCode);
        System.out.println(name);
        if (opCode == 3) {
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

        SecureRandom random = new SecureRandom();

        //Computes the hash of the original payload
        MessageDigest hash = MessageDigest.getInstance(hashName);
        hash.update(payload);

        if (username.equals(""))
            getUsername(payload);
        dataStream.write(username.getBytes());
        dataStream.write(sequenceNumb++);
        dataStream.write(random.nextInt());
        dataStream.write(payload);
        dataStream.write(hash.digest());

        dataStream.close();
        byte[] data = byteStream.toByteArray();

        //TODO Apply cryptography


        return data;
    }

    private void getUsername(byte[] message) throws IOException {
        DataInputStream istream = new DataInputStream(new ByteArrayInputStream(message, 0, message.length));

        istream.readLong();
        istream.readInt();
        username = istream.readUTF();
    }

    private byte[] applyCrypto(String mode, byte[] data) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithmName + "/" + encryptMode + "/" + padding);
        Key key = keystore.getKey(chatID, PASSWORD.toCharArray());

        if (!encryptMode.equals("GCM")) {

            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            if (mode.equalsIgnoreCase("Encrypt"))
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            else
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        } else {
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, ivBytes);

            if (mode.equalsIgnoreCase("Encrypt"))
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
            else
                cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
        }

        return cipher.doFinal(data);
    }

    private byte[] generateMAC(byte[] payload) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, InvalidKeyException {

        Mac mac = Mac.getInstance(macName);
        Key macKey = keystore.getKey(chatID + "H", PASSWORD.toCharArray());
        mac.init(macKey);

        mac.update(payload);

        return mac.doFinal();
    }

}
