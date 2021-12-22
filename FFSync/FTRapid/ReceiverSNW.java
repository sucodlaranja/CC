package FTRapid;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReceiverSNW {

    // Data will be sent from this socket to the address and port bellow.
    private final DatagramSocket socket;
    private final InetAddress ADDRESS;
    private int PORT; // starts as listenerPort and then represents senders port.

    // Mode can be FILE, LOGS or GUIDE.
    private final int MODE;

    // Filename/path.
    private final String filepath;
    private final String filename;

    // Requesting and receiving files.
    public ReceiverSNW(InetAddress address, int handlerPort, String filepath, String filename){

        // Local vars.
        DatagramSocket localSocket = null;
        try{
            localSocket = new DatagramSocket();
        }
        catch (SocketException e){
            System.out.println("Failed to create socket.");
            e.printStackTrace();
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

    // LOGS and GUIDE.
    public ReceiverSNW(DatagramSocket socket, int MODE, InetAddress address, int handlerPort){
        this.socket = socket;
        this.MODE = MODE;
        this.ADDRESS = address;
        this.PORT = handlerPort;
        this.filename = "";
        this.filepath = "";
    }

    // Request the file to the other peer. Sending packet to the transfer handler listener.
    // Wait and approve META by sending an ACK packet to the sender socket.
    public byte[] requestAndReceive() {

        DatagramPacket packet2beSent = null;
        if(this.MODE == FTRapidPacket.FILE)
            // Create RQF (request file) packet. Only useful if MODE=FILE. PORT corresponds to listener port.
            packet2beSent = FTRapidPacket.getRQFPacket(this.ADDRESS, this.PORT, this.filename);
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
                System.out.println("Received packet " + index + "/" + (N_PACKETS-1)); // TODO: REMOVE
            }

            // Change previous sequence number.
            prevSeqNum = prevSeqNum == 0? 1 : 0;
        }

        // Acknowledge last data packet - only once.
        // If the message doesn't get through, the sender will still be able to finish.
        DatagramPacket ACK = FTRapidPacket.getACKPacket(ADDRESS, PORT, prevSeqNum);
        try {
            this.socket.send(FTRapidPacket.encode(ACK, FTRapidPacket.DEFAULT_MUTUAL_SECRET));
        }
        catch (IOException e){
            e.printStackTrace();
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
            catch (IOException e){
                e.printStackTrace();
            }
        }

        System.out.println(this.filepath + " was received."); // TODO: REMOVE

        return fileBytes.clone();
    }

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