package SecureProtocol;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;

public class SMCPPacket {

    static final String FILE_LOCATION = "D:\\Rafael Gameiro\\Documents\\Programming\\SRSC\\TP1\\src\\SecureProtocol\\Config\\MessageLog.conf";
    static final String HASH_FUNCTION = "SHA256";

    static final int GCM_TAG_LENGTH = 12;
    static final String[] ENCODE_MODE = {"206", "219", "257", "306", "319", "327", "506", "519", "527"};

    private String chatID;
    private int sequenceNumb;
    private String username;
    private int packetLen;
    private int currMsgLen;
    private int macLen;
    private int offset;
    private int messageOff;

    private KeyStore keystore;

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

    public SMCPPacket() {

    }

    private boolean checkMessageIntegrity(byte[] data) throws IOException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, InvalidKeyException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataInputStream instream = new DataInputStream(new ByteArrayInputStream(data, 0, data.length));
        DataOutputStream outStream = new DataOutputStream(byteStream);

        byte[] mac = new byte[macLen];

        byte[] buffer = new byte[packetLen];
        instream.read(buffer, 0, packetLen);
        outStream.write(buffer, 0, packetLen);

        instream.read(mac, 0, macLen);
        byte[] computedMac = generateMAC(byteStream.toByteArray());

        return Arrays.equals(mac, computedMac);
    }

    private byte[] checkPayloadIntegrity(byte[] data) throws NoSuchAlgorithmException, IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataInputStream instream = new DataInputStream(new ByteArrayInputStream(data, 0, data.length));
        DataOutputStream outStream = new DataOutputStream(byteStream);

        byte[] hash = new byte[macLen];
        byte[] payload = new byte[currMsgLen];

        System.out.println(payload);
        //read everything to inside the instream
        instream.skipBytes(messageOff);
        instream.read(payload, 0, currMsgLen);
        System.out.println(payload);


        MessageDigest computedHash = MessageDigest.getInstance(hashName);
        computedHash.update(payload);
        instream.readFully(hash);

        return Arrays.equals(hash, computedHash.digest()) ? payload : null;
    }

    private boolean chatAuthentication(String address) {

        FileReader fr;
        try {
            fr = new FileReader(new File(FILE_LOCATION));

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

    private byte[] computePayload(byte[] payload, int encode) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InvalidKeyException, IOException {
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
        for(int i = 0; i < ivBytes.length; i+= step)
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
