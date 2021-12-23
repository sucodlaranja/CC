package FTRapid;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import Logs.TransferLogs;

/// This class is used to receive data from the other peer.
/**
 * Receiver class has a method (\ref requestAndReceive) that requests a file, gets it through a Datagram socket and saves it.
 * This method can also receive other kinds of data, such as Logs and Guides.
 */
public class ReceiverSNW {

    
    private final DatagramSocket socket; ///< Data will be sent from this socket to the address and port bellow.
    private final InetAddress ADDRESS;  ///< Peer address.
    private int PORT;                   ///< Starts as a syncHandler/transferHandler port and then represents port of the thread created to handle the transfer.
    private final int MODE;             ///< Mode can be FILE, LOGS or GUIDE.
    private final String filepath;      /// < Filepath is the complete filepath to the file.
    private final String filename;      /// < Filename is the name of the file being received.

    /// Constructor for requesting and receiving files.
    public ReceiverSNW(InetAddress address, int handlerPort, String filepath, String filename){

        // Local vars.
        DatagramSocket localSocket = null;
        try{
            localSocket = new DatagramSocket();
        }
        catch (SocketException e){
            System.out.println("Failed to create socket.");
        }
        finally {
            this.socket = localSocket;
            this.filepath = filepath;
            this.filename = filename;
            this.MODE = FTRapidPacket.FILE;
        }

        this.ADDRESS = address;
        this.PORT = handlerPort;
    }

    /// Constructor for receiving LOGS and GUIDE.
    public ReceiverSNW(DatagramSocket socket, int MODE, InetAddress address, int handlerPort){
        this.socket = socket;
        this.MODE = MODE;
        this.ADDRESS = address;
        this.PORT = handlerPort;
        this.filename = "";
        this.filepath = "";
    }

    ///
    /// Wait and approve META by sending an ACK packet to the sender socket.

    /**
     * Request the file to the other peer. Send a INIT_ACK packet to syncHandler/transferHandler listener.
     * After that, receive META packet and prepare to start receiving file/log/guide.
     * Receive data in stop-and-wait fashion.
     * Save the file in the given path.
     *
     * @return Return list where get(0) is the received data and get(1) is the transfer statistics.
     */
    public List<Object> requestAndReceive() {

        DatagramPacket packet2beSent = null;
        if(this.MODE == FTRapidPacket.FILE)
            // Create INIT_ACK (request file) packet. Only useful if MODE=FILE. PORT corresponds to listener port.
            packet2beSent = FTRapidPacket.getINITACKPacket(this.ADDRESS, this.PORT, this.filename);
        else if(this.MODE == FTRapidPacket.LOGS)
            // Create ACK packet. Useful if MODE=LOGS. We need to aknowledge INIT_ACK packet and wait for META.
            packet2beSent = FTRapidPacket.getACKPacket(this.ADDRESS, this.PORT, FTRapidPacket.CONTROL_SEQ_NUM);

        // Send RQF packet and wait for approval a.k.a. META packet.
        // Save META packet.
        // In case we're waiting to receive a LOG or Guide, we won't send RQF packets...RQF is only used for files...
        FTRapidPacket meta_FTRapidPacket = FTRapidPacket.sendAndWaitLoop(this.socket, packet2beSent, FTRapidPacket.META ,this.MODE, FTRapidPacket.CONTROL_SEQ_NUM, false);
        if(meta_FTRapidPacket == null)
            return null;

        // META info (mode, filesize and filename if needed) is in ftRapidPacket. General info below.
        int FILESIZE = meta_FTRapidPacket.getFilesize();
        int N_PACKETS = (int) Math.ceil((double) FILESIZE / FTRapidPacket.DATA_CONTENT_SIZE);
        int LAST_PACKET_DATA_SIZE = FILESIZE - (N_PACKETS - 1) * FTRapidPacket.DATA_CONTENT_SIZE;


        // All received byte packets will be here.
        List<byte[]> allPackets = new ArrayList<>(N_PACKETS);

        // Change PORT
        this.PORT = meta_FTRapidPacket.getPort();

        long start = System.currentTimeMillis();

        // First Acknowledgement is for the META packet.
        // Wait and receive DATA packets and send ACK packets with respective sequence numbers.
        int index, prevSeqNum;
        for (index = 0, prevSeqNum = 1; index < N_PACKETS; ++index) {
            DatagramPacket ack_packet = FTRapidPacket.getACKPacket(ADDRESS, PORT, prevSeqNum);

            FTRapidPacket data = FTRapidPacket.sendAndWaitLoop(this.socket, ack_packet, FTRapidPacket.DATA, this.MODE, prevSeqNum, true);

            if(data == null)
                return null;
            else{
                allPackets.add(index, data.getDataBytes().clone());
            }

            // Change previous sequence number.
            prevSeqNum = prevSeqNum == 0? 1 : 0;
        }

        long end = System.currentTimeMillis();

        // Acknowledge last data packet - only once.
        // If the message doesn't get through, the sender will still be able to finish.
        DatagramPacket ACK = FTRapidPacket.getACKPacket(ADDRESS, PORT, prevSeqNum);
        try {
            this.socket.send(FTRapidPacket.encode(ACK));
        }
        catch (IOException ignored){
        }


        // Collapse allPackets into byte[].
        byte[] fileBytes = collapse(allPackets, LAST_PACKET_DATA_SIZE);
        allPackets.clear();

        // If FILE mode, save file.
        if(this.MODE == FTRapidPacket.FILE){
            try {
                // Take name of the file out of filepath. Filepath is the full path of the file.
                StringBuilder finalDirectoryPath = new StringBuilder("/");
                String[] splitFilepath = this.filepath.split("/");
                for(int i = 0; i < splitFilepath.length - 1; ++i)
                    finalDirectoryPath.append(splitFilepath[i]).append("/");

                Files.createDirectories(Paths.get(finalDirectoryPath.toString()));

                FileOutputStream fos = new FileOutputStream(this.filepath);
                fos.write(fileBytes, 0, fileBytes.length);
                fos.close();
            }
            catch (FileNotFoundException e){
                System.out.println("Receiver: " + this.filepath + " is not a valid filepath.");
            }
            catch (IOException ignored){
            }
        }


        long elapsedTime = end - start;
        double bitsPSeg = ((fileBytes.length * 0.001)  / (elapsedTime * 0.001)); // bytes por segundo

        // Received file and stats. Don't send filebytes if MODE=FILE.
        List<Object> retList = new ArrayList<>(2);
        retList.add(0, this.MODE == FTRapidPacket.FILE? null : fileBytes.clone());
        retList.add(1,new TransferLogs(this.filename,false,elapsedTime,bitsPSeg));

        return retList;
    }

    /**
     * Takes all the packets that the \ref requestAndReceive method received and puts them all in the same byte array.
     *
     * @param packetsSplit All the packets received in \ref requestAndReceive method.
     * @param LAST_PACKET_DATA_SIZE Size of the last data packet.
     * @return Returns all the packets merged into a single byte array.
     */
    private byte[] collapse(List<byte[]> packetsSplit, int LAST_PACKET_DATA_SIZE) {
        byte[] ret = new byte[(packetsSplit.size() - 1) * FTRapidPacket.DATA_CONTENT_SIZE + LAST_PACKET_DATA_SIZE];

        int i;
        for(i = 0; i < packetsSplit.size() - 1; ++i)
            System.arraycopy(packetsSplit.get(i), 0, ret, i * FTRapidPacket.DATA_CONTENT_SIZE, FTRapidPacket.DATA_CONTENT_SIZE);

        int offset = i * FTRapidPacket.DATA_CONTENT_SIZE;
        byte[] buf = packetsSplit.get(i);
        for(int k = offset, j = 0; k < offset + LAST_PACKET_DATA_SIZE; ++k, j++)
            ret[k] = buf[j];

        return ret;
    }

}