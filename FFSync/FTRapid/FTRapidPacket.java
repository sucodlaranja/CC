package FTRapid;

import Listener.Listener;
import Syncs.SyncInfo;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

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


    private final InetAddress peerAddress;
    private final Integer key;
    private final int OPCODE;
    private final int randomNumber;
    private final int port; // port of datagram packet.
    private final int transferMODE;
    private final int filesize;
    private final int sequenceNumber;
    private final String filename;
    private final byte[] dataBytes;

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
            local_dataBytes = dataStr.split("@", 3)[2].getBytes(StandardCharsets.UTF_8);
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

    public static Integer calculateFTRapidPacketKey(String filename, InetAddress address){
        return (filename + address.toString()).hashCode();
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
        return peerAddress;
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
        return this.dataBytes;
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
    // Get DATA packet: 4@seqNum@data
    public static DatagramPacket getDATAPacket(InetAddress ADDRESS, int PORT, int seqNum, byte[] data){
        // DATA packet header info.
        byte[] dataHeader = (DATA + "@" + seqNum + "@").getBytes(StandardCharsets.UTF_8);

        // Copy mode, seqNum and data all to one byte array.
        byte[] finalData = new byte[dataHeader.length + data.length];
        System.arraycopy(dataHeader, 0, finalData, 0, dataHeader.length);
        System.arraycopy(data, 0, finalData, dataHeader.length, data.length);

        return new DatagramPacket(finalData, finalData.length, ADDRESS, PORT);
    }
    // Get RQF packet: 5@filename@
    public static DatagramPacket getRQFPacket(InetAddress ADDRESS, int PORT, String filename){
        byte[] packetB = (RQF + "@" + filename + "@").getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(packetB, packetB.length, ADDRESS, PORT);
    }


    public static DatagramPacket sendAndWait(DatagramSocket socket, DatagramPacket packet) {
        try {
            if(packet != null)
                socket.send(packet);

            byte[] receiveData = new byte[FTRapidPacket.BUFFER_SIZE];
            DatagramPacket received = new DatagramPacket(receiveData, receiveData.length);
            socket.setSoTimeout(3500);
            socket.receive(received);

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

    public static FTRapidPacket sendAndWaitLoop(DatagramSocket socket, DatagramPacket packet, int OPCODE, int MODE, int seqNum){
        int timeOutCounter = 3; // TODO: CHEGA?

        FTRapidPacket ftRapidPacket = null;
        boolean notOver = true;
        while (!socket.isClosed() && notOver) {

            // Send RQF packet if FILE mode is the current mode.
            DatagramPacket received = FTRapidPacket.sendAndWait(socket, packet);

            if(received != null) {
                ftRapidPacket = new FTRapidPacket(received, MODE);

                // If we receive a META, stop the while loop. Check if MODE is correct.
                if (ftRapidPacket.getOPCODE() == OPCODE
                        && ftRapidPacket.getTransferMODE() == MODE
                        && ftRapidPacket.getSequenceNumber() == seqNum)
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
