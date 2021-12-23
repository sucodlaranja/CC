package FTRapid;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/// This class has all the methods related to an FTRapid method.
/**
 * Constructor receives datagram packet and transforms raw information into readable one.
 * This class is used everytime we want to send a packet, to create such packet.
 * It is also where all the MACROS with relevant information about the protocol are.
 * After creating the packet instance, all it's information can be accessed via getters.
 *
 * */
public class FTRapidPacket {

    // Available FTRapid Packets OPCODE's.
    public static final int INVALID = -1;   ///< Invalid packet (not resolved by constructor).
    public static final int ACK = 0;        ///< Acknowledgment packet (carries respective sequence number).
    public static final int INIT = 1;       ///< Carries a random number and a filename, used to start sync.
    public static final int INIT_ACK = 2;   ///< Carries a filename.
    public static final int META = 3;       ///< Used to prepare for a file transfer. Carries file stats.
    public static final int DATA = 4;       ///< Carries data and a sequence number.

    // Packet info.
    public static final int PACKET_SIZE = 8192;                                      ///< Overall packet size.
    public static final int DATA_CONTENT_SIZE = PACKET_SIZE - getDataHeaderSize();  ///< Data size.
    public static final int BUFFER_SIZE = PACKET_SIZE * 2;                          ///< Buffer size used comonly.

    public static final int CONTROL_SEQ_NUM = 2; ///< Sequence number used to acknowledge a control packet.

    public final static int FILE = 0;   ///< FILE MODE of operation. Transfering files.
    public final static int LOGS = 1;   ///< LOGS MODE of operation. Transfering LOGS.
    public final static int GUIDE = 2;  ///< GUIDE MODE of operation. Transfering GUIDE.

    /// Used to encoded and decoded packets. (Currently not being used due to BadPaddingException).
    public static byte[] DEFAULT_MUTUAL_SECRET = "x!A%D*G-KaPdSgVk".getBytes();

    private final InetAddress peerAddress;  ///< Address of the FTRapid packet.
    private final Integer key;              ///< Id of the FTRapid packet.
    private final int OPCODE;               ///< Identifies packet type (DATA, INIT...).
    private final int randomNumber;         ///< Random number, in case OPCODE=INIT.
    private final int port;                 ///< Port of the FTRapid packet.
    private final int transferMODE;         ///< Transfer mode (FILE, LOG, GUIDE), if relevant.
    private final int filesize;             ///< Size of the transfered file, if OPCODE=META.
    private final int sequenceNumber;       ///< Sequence number if OPCODE=DATA or ACK.
    private final String filename;          ///< Name of the file, if relevant.
    private byte[] dataBytes;               ///< Data of DATA packet.

    /// Basic constructor.
    public FTRapidPacket(FTRapidPacket ftRapidPacket){
        this.peerAddress = ftRapidPacket.getPeerAddress();
        this.key = ftRapidPacket.getKey();
        this.OPCODE = ftRapidPacket.getOPCODE();
        this.randomNumber = ftRapidPacket.getRandomNumber();
        this.port = ftRapidPacket.getPort();
        this.transferMODE = ftRapidPacket.getTransferMODE();
        this.filesize = ftRapidPacket.getFilesize();
        this.sequenceNumber = ftRapidPacket.getSequenceNumber();
        this.filename = ftRapidPacket.getFilename();
        this.dataBytes = ftRapidPacket.getDataBytes() == null? null : ftRapidPacket.getDataBytes().clone();
    }

    /**
     * Construct FTRapid packet given a Datagram packet.
     * A very useful constructor, used everytime we receive a datagram packet.
     *
     * @param rcvPacket Received datagram packet.
     * @param knownMode Known mode of operation (File, Log, Guide) if aplicable.
     */
    public FTRapidPacket(DatagramPacket rcvPacket, int knownMode) {
        // Local variables
        int local_randomNumber = -1;
        String local_filename = "";
        int local_transferMODE = knownMode;
        int local_filesize = -1;
        int local_sequenceNumber = CONTROL_SEQ_NUM;
        byte[] local_dataBytes = null;
        InetAddress local_address = rcvPacket.getAddress();

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
            local_transferMODE = Integer.parseInt(data[1]);
            local_filesize = Integer.parseInt(data[2]);
            local_filename = local_transferMODE == FILE ? data[3] : "";
        }
        else if(tempOpcode == DATA){
            if(local_transferMODE == FILE)
                local_sequenceNumber = Integer.parseInt(data[1]);
            local_dataBytes = dataStr.split("@", 3)[2].getBytes();
        }
        else{
            tempOpcode = INVALID;
        }

        this.OPCODE = tempOpcode;
        this.randomNumber = local_randomNumber;
        this.port = rcvPacket.getPort();
        this.filename = local_filename;
        this.transferMODE = local_transferMODE;
        this.filesize = local_filesize;
        this.sequenceNumber = local_sequenceNumber;
        this.dataBytes = local_dataBytes;

        int local_key = 0;
        try {
            local_address = rcvPacket.getAddress() == null? InetAddress.getByName("localhost") : rcvPacket.getAddress();
            local_key = calculateFTRapidPacketKey(this.filename, local_address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        finally {
            this.peerAddress = local_address;
            this.key = local_key;
        }
    }

    /// Key of a given packet: identifies the packet based on a filename and peer address.
    public static Integer calculateFTRapidPacketKey(String filename, InetAddress address){
        return (filename + address.toString()).hashCode();
    }

    /// GET ACK packet: 0@
    public static DatagramPacket getACKPacket(InetAddress ADDRESS, int PORT, int sequenceNumber){
        byte[] packetB = (ACK + "@" + sequenceNumber + "@").getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(packetB, packetB.length, ADDRESS, PORT);
    }

    /// GET INIT packet: 1@random@filename@ => sends always to listener port and peer id saved in syncInfo.
    public static DatagramPacket getINITPacket(InetAddress ADDRESS, int PORT, int random, String filename){
        byte[] packetBytes = (FTRapidPacket.INIT + "@" + random + "@" + filename + "@").getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(packetBytes, packetBytes.length, ADDRESS, PORT);
    }

    /// GET INIT_ACK packet: 2@filename@
    public static DatagramPacket getINITACKPacket(InetAddress ADDRESS, int PORT, String filename){
        byte[] packetBytes = (FTRapidPacket.INIT_ACK + "@" + filename + "@").getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(packetBytes, packetBytes.length, ADDRESS, PORT);
    }

    /// Get META packet: 3@mode@size@ or 3@mode@size@filename@
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

    /// Get DATA packet: 4@seqNum@data
    public static DatagramPacket getDATAPacket(InetAddress ADDRESS, int PORT, int seqNum, byte[] data){
        // DATA packet header info.
        byte[] dataHeader = (DATA + "@" + seqNum + "@").getBytes(StandardCharsets.UTF_8);

        // Copy mode, seqNum and data all to one byte array.
        byte[] finalData = new byte[dataHeader.length + data.length];
        System.arraycopy(dataHeader, 0, finalData, 0, dataHeader.length);
        System.arraycopy(data, 0, finalData, dataHeader.length, data.length);

        return new DatagramPacket(finalData, finalData.length, ADDRESS, PORT);
    }

    /**
     * Used to send a packet through a socket and wait for its response.
     * @param socket Socket where the packet will be sent from and the response received.
     * @param packet Packet to be sent. Can be null, depending on the motivation of the caller of \ref sendAndWait.
     * @return Response of the other peer.
     */
    public static DatagramPacket sendAndWait(DatagramSocket socket, DatagramPacket packet) {
        try {
            if(packet != null)
                socket.send(encode(packet));

            byte[] receiveData = new byte[FTRapidPacket.BUFFER_SIZE];
            DatagramPacket received = new DatagramPacket(receiveData, receiveData.length);
            socket.setSoTimeout(3500);
            socket.receive(decode(received));

            return new DatagramPacket(received.getData(), received.getLength(), received.getAddress(), received.getPort());
        }
        catch (SocketTimeoutException exception) {
            return null;
        }
        catch (IOException e) {
            if(socket.isClosed())
                System.out.println("Receive Socket closed prematurely.");
            else
                e.printStackTrace();
            return null;
        }
    }

    /**
     * Uses the \ref sendAndWait method in loop. Basicly, if the method times out or any error occurs, the loop will run a few times.
     * The received packet is validated here, acording with the given arguments.
     * The returned packet is an FTRapid instance, ready to be used.
     *
     * @param socket Socket to be passed to \ref sendAndWait method.
     * @param packet Packet to be sent. Can be null, depending on the motivation of the caller of \ref sendAndWaitLoop.
     * @param OPCODE Expected OPCODE of the received packet.
     * @param MODE Expected MODE of operation of the received packet.
     * @param seqNum Expected sequence number of the received packet.
     * @param diff Condition is true whene \ref sendAndWaitLoop is called from \ref requestAndReceive.
     * @return Returns the received packet in FTRapid format.
     */
    public static FTRapidPacket sendAndWaitLoop(DatagramSocket socket, DatagramPacket packet, int OPCODE, int MODE, int seqNum, boolean diff){
        int timeOutCounter = 3;

        FTRapidPacket ftRapidPacket = null;
        boolean notOver = true;
        while (!socket.isClosed() && notOver) {

            // Send RQF packet if FILE mode is the current mode.
            DatagramPacket received = FTRapidPacket.sendAndWait(socket, packet);

            if(received != null) {
                ftRapidPacket = new FTRapidPacket(received, MODE);

                if(MODE == FILE && OPCODE == DATA){
                    corruptedBugFix(ftRapidPacket, received.getData());
                }

                if (!diff
                        && ftRapidPacket.getOPCODE() == OPCODE
                        && ftRapidPacket.getTransferMODE() == MODE
                        && ftRapidPacket.getSequenceNumber() == seqNum)
                {
                    notOver = false;
                }
                else if(diff
                        && ftRapidPacket.getOPCODE() == OPCODE
                        && ftRapidPacket.getTransferMODE() == MODE
                        && ftRapidPacket.getSequenceNumber() != seqNum)
                {
                    notOver = false;
                }
            }
            else if (timeOutCounter > 0) {
                timeOutCounter--;
            }
            else{
                System.out.println("Timeout limit exceeded.");
                notOver = false;
            }
        }


        return ftRapidPacket == null? null : new FTRapidPacket(ftRapidPacket);
    }

    /// Used to solve corruption bug in \ref requestAndReceive. Should probably find another fix.
    private static void corruptedBugFix(FTRapidPacket ftRapidPacket, byte[] receivedData){
        // Take the header
        byte[] goodData = new byte[receivedData.length - getDataHeaderSize()];
        System.arraycopy(receivedData, getDataHeaderSize(), goodData, 0, goodData.length);

        ftRapidPacket.dataBytes = goodData.clone();
    }

    /// Basic getter.
    public Integer getKey() {
        return key;
    }
    /// Basic getter.
    public int getOPCODE() {
        return OPCODE;
    }
    /// Basic getter.
    public int getRandomNumber() {
        return randomNumber;
    }
    /// Basic getter.
    public int getPort() {
        return port;
    }
    /// Basic getter.
    public String getFilename() {
        return filename;
    }
    /// Basic getter.
    public InetAddress getPeerAddress() {
        return peerAddress;
    }
    /// Basic getter.
    public int getTransferMODE() {
        return transferMODE;
    }
    /// Basic getter.
    public int getFilesize() {
        return filesize;
    }
    /// Basic getter.
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    /// Get data of DATA packet.
    public byte[] getDataBytes() {
        return this.dataBytes;
    }
    /// Get DATA header packet size.
    public static int getDataHeaderSize(){
        return (DATA + "@" + DATA + "@").getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * Currently not working due to BadPaddingException.
     *
     * @param packet Packet to be encoded.
     * @return Encoded packet (using AES and symetric key).
     */
    public static DatagramPacket encode(DatagramPacket packet){
        return packet;
        /*
        // Encode data.
        byte[] data = packet.getData();
        byte[] encryptedData;
        try {
            Key AES_KEY = new SecretKeySpec(DEFAULT_MUTUAL_SECRET, "AES");
            Cipher senderCipher = Cipher.getInstance("AES");
            senderCipher.init(Cipher.ENCRYPT_MODE, AES_KEY);
            encryptedData = senderCipher.doFinal(data);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException e){
            e.printStackTrace();
            encryptedData = data;
        }

        return new DatagramPacket(encryptedData, encryptedData.length, packet.getAddress(), packet.getPort());
        */
    }

    /**
     * Currently not working due to BadPaddingException.
     *
     * @param packet Packet to be decoded.
     * @return Decoded packet (using AES and symetric key).
     */
    public static DatagramPacket decode(DatagramPacket packet) {
        return packet;
        /*
        byte[] dataBytes = packet.getData();
        byte[] decrypted;
        try {
            Key AES_KEY = new SecretKeySpec(DEFAULT_MUTUAL_SECRET, "AES");

            Cipher receiverCipher = Cipher.getInstance("AES");
            receiverCipher.init(Cipher.DECRYPT_MODE, AES_KEY);
            decrypted = receiverCipher.doFinal(dataBytes);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException |IllegalBlockSizeException | BadPaddingException e){
            e.printStackTrace();
            decrypted = dataBytes;
        }

        return new DatagramPacket(decrypted, decrypted.length, packet.getAddress(), packet.getPort());*/
    }


    // Basic get hashCode method.
    public int hashCode() {
        return this.key;
    }

    /// Basic equals method.
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
