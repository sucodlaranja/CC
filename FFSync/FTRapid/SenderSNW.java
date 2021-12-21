package FTRapid;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to send DATA packets. DATA packets can contain a portion of a file, a Log or a Guide.
 * To send a file we only need its path. The path must be given to the constructor.
 * To send a Log or a File, a byte[] must be given to the constructor.
 *
 * */
public class SenderSNW {

    // Mode of operation (can be FILE, LOG, GUIDE).
    public final int MODE;

    // Filepath (if needed).
    private final String FILEPATH;

    // Data to be sent.
    private final byte[] dataToSend;

    // Data will be sent from this socket to the address bellow.
    private final DatagramSocket socket;
    private final InetAddress ADDRESS;
    private final int PORT;

    // Amount of maximum timeouts before exiting.
    private final int timeOutCounter;               // TODO: CHEGA 3?

    // Sending File
    public SenderSNW(InetAddress address, int PORT, String filepath){
        // Local variables.
        DatagramSocket localSocket = null;
        int localMode = FTRapidPacket.ERROR;
        String localPath = "";
        byte[] localData = null;

        // Creating socket and reading file...
        try{
            localSocket = new DatagramSocket();
            localData = Files.readAllBytes(Paths.get( filepath ));
            localPath = filepath;
            localMode = FTRapidPacket.FILE;
        }
        catch (SocketException e){
            System.out.println("Failed to create socket.");
            e.printStackTrace();
        }
        catch (OutOfMemoryError e){
            System.out.println("File is to big to be transferred.");
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        finally {
            this.dataToSend = localData;
            this.FILEPATH = localPath;
            this.MODE = localMode;
            this.socket = localSocket;
            this.ADDRESS = address;
            this.PORT = PORT;
            this.timeOutCounter = 3;
        }
    }

    // Sending LOG or GUIDE.
    public SenderSNW(DatagramSocket socket, InetAddress address, int PORT, byte[] data, int mode){
        this.socket = socket;
        this.ADDRESS = address;
        this.PORT = PORT;
        this.dataToSend = data.clone();
        this.MODE = mode;
        this.FILEPATH = "";
        this.timeOutCounter = 3;
    }

    public void send(){
        // Let's split the byte[] dataToSend into many packets (use packet size determined in FTRapidPacket class).
        List<byte[]> allPackets = split(this.dataToSend);

        // Create META packet.
        DatagramPacket metaPacket = FTRapidPacket.getMETAPacket(ADDRESS, PORT, this.MODE, dataToSend.length, this.FILEPATH);

        // Send META packet and wait for approval.
        List<Object> workerResult =
                FTRapidPacket.worker(this.socket, metaPacket, timeOutCounter, this.MODE, FTRapidPacket.ACK, 1); // TODO: CHECK RETURN VALUE

        // Check if we should continue.
        if(workerResult.size() < 1 || workerResult.get(0).equals(1)){
            return; // TODO: GTFO
        }

        // Send data to the other peer.
        int seqNum = 0; // sequence number can only be 0/1.
        for (byte[] packData : allPackets) {
            // Create data packet.
            DatagramPacket dataPacket = FTRapidPacket.getDATAPacket(this.ADDRESS, this.PORT, seqNum, packData);

            // Send data packet and wait for ACK. Do this in a stop-and-wait manner.
            workerResult =
                    FTRapidPacket.worker(this.socket, dataPacket, timeOutCounter, this.MODE, FTRapidPacket.ACK, seqNum); // TODO: CHECK RETURN VALUE

            // Check if we should continue.
            if(workerResult.size() < 1 || workerResult.get(0).equals(1)){
                return; // TODO: GTFO
            }

            // Change sequence number.
            seqNum = seqNum == 0? 1 : 0;
        }

        // Close this socket only if in FILE mode - not going to be used again.
        if(this.MODE == FTRapidPacket.FILE)
            socket.close();

        // ALL IS OK.
    }

    // Split byte[] into list. Packet Size defined in FTRapidPacket class.
    private List<byte[]> split(byte[] data) {
        // Size of each packet.

        // Number of packets
        int nPackets = (int) Math.ceil((double) data.length / FTRapidPacket.DATA_CONTENT_SIZE);

        // Create and fill list to be returned.
        List<byte[]> retList = new ArrayList<>(nPackets);
        int i;
        for (i = 0; i < nPackets - 1; i++) {
            retList.add(i, new byte[FTRapidPacket.DATA_CONTENT_SIZE]);
            System.arraycopy(data, i * FTRapidPacket.DATA_CONTENT_SIZE, retList.get(i), 0, retList.get(i).length);
        }

        // Create last packet - usually with a different size.
        int lastPacketSize = data.length - (nPackets-1) * FTRapidPacket.DATA_CONTENT_SIZE;
        retList.add(i, new byte[lastPacketSize]);
        System.arraycopy(data, i * FTRapidPacket.DATA_CONTENT_SIZE, retList.get(i), 0, retList.get(i).length);

        return retList;
    }

}
