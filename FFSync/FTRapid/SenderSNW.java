package FTRapid;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;

import Logs.TransferLogs;

/// This class is used to send data to another peer.
/**
 * Used to send DATA packets. DATA packets can contain a portion of a file, a Log or a Guide.
 * To send a file we only need its path. The path must be given to the constructor.
 * To send a Log or a File, a byte[] must be given to the constructor.
 *
 * */
public class SenderSNW {

    
    public final int MODE;               ///< Mode of operation (can be FILE, LOG, GUIDE).
    private final String FILEPATH;       ///< Filepath (if needed).
    private final byte[] dataToSend;     ///< Data to be sent.
    private final DatagramSocket socket; ///< Data will be sent from this socket to the address bellow.
    private final InetAddress ADDRESS;   ///< Peer address.
    private final int PORT;              ///< Port where to send file.
    private boolean allOK;               ///< Check if the constructor did its job correctly before starting \ref send.

    /// Constructor for sending FILE
    public SenderSNW(InetAddress address, int PORT, String filepath){
        // Local variables.
        DatagramSocket localSocket = null;
        int localMode = FTRapidPacket.INVALID;
        String localPath = "";
        byte[] localData = null;
        allOK = false;


        // Creating socket and reading file...
        try{
            localSocket = new DatagramSocket();
            FileInputStream fis = new FileInputStream(filepath);
            localData = fis.readAllBytes();
            fis.close();
            localPath = filepath;
            localMode = FTRapidPacket.FILE;
            allOK = true; // only ok if exceptions were not thrown.
        }
        catch (SocketException e){
            System.out.println("Failed to create socket.");
        }
        catch (OutOfMemoryError e){
            System.out.println("File is to big to be transferred.");
        }
        catch (NoSuchFileException e){
            System.out.println("File " + filepath + " was not found.");
        }
        catch (IOException ignored){
        }
        finally {
            this.dataToSend = localData;
            this.FILEPATH = localPath;
            this.MODE = localMode;
            this.socket = localSocket;
            this.ADDRESS = address;
            this.PORT = PORT;
        }
    }

    /// Constructor for sending LOG or GUIDE.
    public SenderSNW(DatagramSocket socket, InetAddress address, int PORT, byte[] data, int mode){
        this.socket = socket;
        this.ADDRESS = address;
        this.PORT = PORT;
        this.dataToSend = data.clone();
        this.MODE = mode;
        this.FILEPATH = "";
        this.allOK = true;
    }

    /**
     * Send file to another peer, after sending META packet.
     * Files are sent in stop-and-wait fashion.
     *
     * @return Returning transfer stats such as time of transfer.
     */
    public TransferLogs send(){
        // Constructor failed.
        if(!allOK)
            return null;

        // Split the byte[] dataToSend into many packets (use packet size determined in FTRapidPacket class).
        List<byte[]> allPackets = split(this.dataToSend);

        DatagramPacket metaPacket = FTRapidPacket.getMETAPacket(ADDRESS, PORT, this.MODE, dataToSend.length, this.FILEPATH);

        if(FTRapidPacket.sendAndWaitLoop(this.socket, metaPacket, FTRapidPacket.ACK, this.MODE, 1, false) == null)
            return null;

        // Start timer
        long start = System.currentTimeMillis();

        // Send data to the other peer.
        int seqNum = 0; // sequence number can only be 0/1.
        for (byte[] packData : allPackets) {

            DatagramPacket dataPacket = FTRapidPacket.getDATAPacket(this.ADDRESS, this.PORT, seqNum, packData);
            if(FTRapidPacket.sendAndWaitLoop(this.socket, dataPacket, FTRapidPacket.ACK, this.MODE, seqNum, false) == null)
                return null;

            // Change sequence number.
            seqNum = seqNum == 0? 1 : 0;
        }

        // Transfer end time.
        long end = System.currentTimeMillis();

        // Close this socket only if in FILE mode - not going to be used again.
        if(this.MODE == FTRapidPacket.FILE)
            socket.close();


        // Printing stats
        long elapsedTime = end - start;
        double bitsPSeg = ((this.dataToSend.length * 0.001)  / (elapsedTime * 0.001)); // bytes por segundo

        return new TransferLogs(this.FILEPATH,true,elapsedTime,bitsPSeg);
    }


    /**
     * Split byte[] into list of byte[] each with a specific packet size given in FTRapidPacket class.
     * Used to split a file into data packets ready to be send to another peer.
     *
     * @param data Data to be split.
     * @return List of byte[] each with a specific size.
     */
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
