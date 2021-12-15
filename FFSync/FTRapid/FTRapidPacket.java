package FTRapid;

import Listener.Listener;
import Syncs.SyncInfo;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

/**
 * Constructor receives datagram packet and transforms byte stream to readable and accessible information.
 *
 * */
public class FTRapidPacket {

    public static final int INVALID = -1;
    public static final int ACK = 0;
    public static final int INIT = 1;
    public static final int INIT_ACK = 2;
    public static final int META = 3;
    public static final int DATA = 4;
    public static final int RQF = 5;

    public static final int PACKET_SIZE = 512;
    public static final int DATA_HEADER_SIZE = (DATA + "@" + DATA + "@").getBytes(StandardCharsets.UTF_8).length; // 2 char's + 2 int's.
    public static final int DATA_CONTENT_SIZE = PACKET_SIZE - DATA_HEADER_SIZE;
    public static final int BUFFER_SIZE = 1024;
    public static final int ACK_BUFFER = 8;

    // This is the sequence number used to aknowledge a control packet.
    // DATA packets are aknowledge with sequence numbers 0/1.
    public static final int CONTROL_SEQ_NUM = 2;

    public final static int ERROR = -1;
    public final static int FILE = 0;
    public final static int LOGS = 1;
    public final static int GUIDE = 2;

    // Default Mutual Secret: used to check data integrity and also to decryption.
    public final static byte[] DEFAULT_MUTUAL_SECRET = "<16bytekeyhere!>".getBytes(StandardCharsets.UTF_8);
    public final static int DIGEST_SIZE = 20;

    private final InetAddress PeerAddress;
    private final Integer key;
    private final int OPCODE;
    private final int randomNumber;
    private final int port; // port of datagram packet.
    private final int transferMODE;
    private final int filesize;
    private final int sequenceNumber;
    private final String filepath;
    private final String filename;
    private final byte[] dataBytes;
    private final byte[] digest;

    public FTRapidPacket(DatagramPacket rcvPacket, int knownMode) {
        // Local variables
        int local_randomNumber = -1;
        String local_filename = "";
        int local_transferMODE = knownMode;
        int local_filesize = -1;
        String local_filepath = "";
        int local_sequenceNumber = -1;
        byte[] local_dataBytes = null;
        byte[] local_digest = new byte[DIGEST_SIZE];


        // Parse data
        String dataStr = new String(rcvPacket.getData(), StandardCharsets.UTF_8);
        String[] data = dataStr.split("@");

        int tempOpcode;
        try{
            tempOpcode = Integer.parseInt(data[0]);
        }
        catch (NumberFormatException e){
            tempOpcode = INVALID;
        }

        if(tempOpcode == ACK){
            local_sequenceNumber = Integer.parseInt(data[1]);
        }
        else if(tempOpcode == INIT && data.length > 2){
            // 1@random_int@filename@
            local_randomNumber = Integer.parseInt(data[1]);
            local_filename = data[2];
        }
        else if(tempOpcode == INIT_ACK && data.length > 1){
            // 2@filename@
            local_filename = data[1];
        }
        else if(tempOpcode == META){
            local_filename = data[1];
            local_transferMODE = Integer.parseInt(data[1]);
            local_filesize = Integer.parseInt(data[2]);
            local_filepath = local_transferMODE == FILE ? data[3] : "";
        }
        else if(tempOpcode == DATA){
            // Data: 4@seqNum@digest+data
            if(local_transferMODE == FILE)
                local_sequenceNumber = Integer.parseInt(data[1]);

            byte[] digestAndData = dataStr.split("@", 3)[2].getBytes(StandardCharsets.UTF_8);

            // Copy the first 20 bytes from digestAndData to the local_digest variable.
            System.arraycopy(digestAndData, 0, local_digest, 0, DIGEST_SIZE);

            // Copy the rest of the content data to local_dataBytes.
            local_dataBytes = new byte[digestAndData.length - DIGEST_SIZE];
            System.arraycopy(digestAndData, DIGEST_SIZE, local_dataBytes, 0, local_dataBytes.length);
        }
        else if(tempOpcode == RQF){
            local_filename = data[1];
        }
        else{
            tempOpcode = INVALID;
            System.out.println("OPCODE not found. Packet is invalid.");
        }

        this.OPCODE = tempOpcode;
        this.randomNumber = local_randomNumber;
        this.port = rcvPacket.getPort();
        this.filename = local_filename;
        this.filepath = local_filepath;
        this.transferMODE = local_transferMODE;
        this.filesize = local_filesize;
        this.sequenceNumber = local_sequenceNumber;
        this.PeerAddress = rcvPacket.getAddress();
        this.key = calculateFTRapidPacketKey(this.filename, this.PeerAddress);
        this.dataBytes = local_dataBytes;
        this.digest = local_digest;
    }

    public static Integer calculateFTRapidPacketKey(String filename, InetAddress address){
        return (filename + address.toString()).hashCode();
    }


    public byte[] getDigest() {
        return digest.clone();
    }
    public Integer getKey() {
        return key;
    }
    public int getOPCODE() {
        return OPCODE;
    }
    public int getRandomNumber() {
        return randomNumber;
    }
    public int getPort() {
        return port;
    }
    public String getFilename() {
        return filename;
    }
    public InetAddress getPeerAddress() {
        return PeerAddress;
    }
    public int getTransferMODE() {
        return transferMODE;
    }
    public int getFilesize() {
        return filesize;
    }
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    public byte[] getDataBytes() {
        // Get data of DATA packet.
        return this.dataBytes.clone();
    }

    // GET ACK packet: 0@
    public static DatagramPacket getACKPacket(InetAddress ADDRESS, int PORT, int sequenceNumber){
        byte[] packetB = (ACK + "@" + sequenceNumber + "@").getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(packetB, packetB.length, ADDRESS, PORT);
    }
    // GET INIT packet: 1@random@filename@ => sends always to listener port and peer id saved in syncInfo.
    public static DatagramPacket getINITPacket(int random, SyncInfo syncInfo){
        byte[] packetBytes = (FTRapidPacket.INIT + "@" + random + "@" + syncInfo.getFilename() + "@").getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(packetBytes, packetBytes.length, syncInfo.getIpAddress(), Listener.LISTENER_PORT);
    }
    // GET INIT_ACK packet: 2@filename@ => always sends to listener port and peer id saved in syncInfo.
    public static DatagramPacket getINITACKPacket(SyncInfo syncInfo){
        byte[] packetBytes = (FTRapidPacket.INIT_ACK + "@" + syncInfo.getFilename() + "@").getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(packetBytes, packetBytes.length, syncInfo.getIpAddress(), Listener.LISTENER_PORT);
    }
    // Get META packet: 3@mode@size@ or 3@mode@size@filename@
    public static DatagramPacket getMETAPacket(InetAddress ADDRESS, int PORT, int MODE, int size, String filename){
        // Information we need to send: MODE, data-size, filename (if needed).
        String metaStr;
        if(MODE == FILE){
            metaStr = META + "@" + MODE + "@" + size + "@" + filename + "@";
        }
        else if(MODE == LOGS || MODE == GUIDE){
            metaStr = META + "@" + MODE + "@" + size + "@";
        }
        else{
            metaStr = "@ERROR@";
        }

        byte[] metaBytes = metaStr.getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(metaBytes, metaBytes.length, ADDRESS, PORT);
    }
    // Get DATA packet, digest_size=20bytes: 4@seqNum@digest+data
    public static DatagramPacket getDATAPacket(InetAddress ADDRESS, int PORT, int seqNum, byte[] data, byte[] secret){
        // DATA packet header info.
        byte[] dataHeader = (DATA + "@" + seqNum + "@").getBytes(StandardCharsets.UTF_8);

        // Encode data.
        byte[] encryptedData;
        try {
            Key aesKey = new SecretKeySpec(secret, "AES");
            Cipher senderCipher = Cipher.getInstance("AES");
            senderCipher.init(Cipher.ENCRYPT_MODE, aesKey);
            encryptedData = senderCipher.doFinal(data);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException e){
            System.out.println("Data not encrypted. Something went wrong with the encoding.");
            encryptedData = data;
        }

        // Get digest. It will grant data integrity.
        byte[] senderDigest;
        try{
            Key hmacKey = new SecretKeySpec(secret, "HmacSHA1");
            Mac senderMac = Mac.getInstance("HmacSHA1");
            senderMac.init(hmacKey);
            senderDigest = senderMac.doFinal(encryptedData);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException e){
            System.out.println("Wrong HMAC algorithm. Digest failed to be created.");
            senderDigest = new byte[DIGEST_SIZE]; // HmacSHA1 has 20 bytes of length.
        }

        // Copy mode, seqNum and data all to one byte array.
        byte[] finalData = new byte[dataHeader.length + senderDigest.length + encryptedData.length];
        System.arraycopy(dataHeader, 0, finalData, 0, dataHeader.length);
        System.arraycopy(senderDigest, 0, finalData, dataHeader.length, senderDigest.length);
        System.arraycopy(encryptedData, 0, finalData, dataHeader.length + senderDigest.length, encryptedData.length);

        return new DatagramPacket(finalData, finalData.length, ADDRESS, PORT);
    }
    // Get RQF packet: 5@filename@
    public static DatagramPacket getRQFPacket(InetAddress ADDRESS, int PORT, String filename){
        byte[] packetB = (RQF + "@" + filename + "@").getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(packetB, packetB.length, ADDRESS, PORT);
    }


    @Override
    public int hashCode() {
        return this.key;
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (o == null) return false;
        if (this.getClass() != o.getClass()) return false;
        FTRapidPacket ftrPacket = (FTRapidPacket) o;

        return this.key.equals(ftrPacket.key)
                && this.OPCODE == ftrPacket.OPCODE
                && this.randomNumber == ftrPacket.randomNumber
                && this.filename.equals(ftrPacket.filename);
    }

}
