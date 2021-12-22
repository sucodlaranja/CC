package FTRapid;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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

    // Check if sender constructor did its job correctly.
    private boolean allOK;

    // Sending File
    public SenderSNW(InetAddress address, int PORT, String filepath){
        // Local variables.
        DatagramSocket localSocket = null;
        int localMode = FTRapidPacket.ERROR;
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
            e.printStackTrace();
        }
        catch (OutOfMemoryError e){
            System.out.println("File is to big to be transferred.");
            e.printStackTrace();
        }
        catch (NoSuchFileException e){
            System.out.println("File " + filepath + " was not found.");
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
        this.allOK = true;
    }

    // Send file to another peer, after sending META packet.
    // TODO: RETURN SOMETHING USEFUL -> sent/not sent, stats...
    public void send(){
        // Constructor failed.
        if(!allOK)
            return;

        // Let's split the byte[] dataToSend into many packets (use packet size determined in FTRapidPacket class).
        List<byte[]> allPackets = split(this.dataToSend);

        DatagramPacket metaPacket = FTRapidPacket.getMETAPacket(ADDRESS, PORT, this.MODE, dataToSend.length, this.FILEPATH);

        if(FTRapidPacket.sendAndWaitLoop(this.socket, metaPacket, FTRapidPacket.ACK, this.MODE, 1, false) == null)
            return;


        // TODO: start timer
        long start = System.currentTimeMillis();

        int counter = 0;
        // Send data to the other peer.
        int seqNum = 0; // sequence number can only be 0/1.
        for (byte[] packData : allPackets) {

            DatagramPacket dataPacket = FTRapidPacket.getDATAPacket(this.ADDRESS, this.PORT, seqNum, packData);
            if(FTRapidPacket.sendAndWaitLoop(this.socket, dataPacket, FTRapidPacket.ACK, this.MODE, seqNum, false) == null)
                return;

            System.out.println("Sended packet " + counter + "/" + (allPackets.size() - 1)); // TODO: REMOVE
            counter++;// TODO: REMOVE

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
        System.out.println(this.FILEPATH + " was sent in " + elapsedTime + " mili seconds.");
        System.out.println("Average speed of " + ((this.dataToSend.length * 0.001) / (elapsedTime * Math.pow(10, -9))) + " KB/s");


        /*
        Packet = 512
        * /home/rubensas/Desktop/teste/velho_big.txt was sent in 10254 mili seconds.
        * Average speed of 2.1140403744880047E8 KB/s
        *
        Packet = 800
        * /home/rubensas/Desktop/teste/velho_big.txt was sent in 7514 mili seconds.
        * Average speed of 2.884930795847751E8 KB/s
        * */

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
