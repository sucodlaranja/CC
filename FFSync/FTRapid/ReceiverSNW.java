package FTRapid;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
    // TODO: simplify code -> "while(timeout){}" piece of code is similar...
    public byte[] requestAndReceive() {

        // Create RQF (request file) packet. Only useful if MODE=FILE. PORT corresponds to listener port.
        DatagramPacket RQF = null;
        if(this.MODE == FTRapidPacket.FILE)
            RQF = FTRapidPacket.getRQFPacket(this.ADDRESS, this.PORT, this.filename);

        // Create ACK packet. Useful if MODE=LOGS. We need to aknowledge INIT_ACK packet and wait for META.
        DatagramPacket ACK_CONTROL = null;
        if(this.MODE == FTRapidPacket.LOGS)
            ACK_CONTROL = FTRapidPacket.getACKPacket(this.ADDRESS, this.PORT, FTRapidPacket.CONTROL_SEQ_NUM);

        // Send RQF packet and wait for approval a.k.a. META packet.
        // Save META packet.
        // In case we're waiting to receive a LOG or Guide, we won't send RQF packets...RQF is only used for files...
        FTRapidPacket meta_FTRapidPacket = null;
        boolean timedOut = true;
        while (timedOut) {
            try     {
                // Send RQF packet if FILE mode is the current mode.
                if(this.MODE == FTRapidPacket.FILE)
                    socket.send(RQF);
                // Aknowledge INIT_ACK with a control ACK and wait for META.
                else if(this.MODE == FTRapidPacket.LOGS)
                    socket.send(ACK_CONTROL);

                // Create a byte array to receive META.
                byte[] receiveData = new byte[FTRapidPacket.BUFFER_SIZE];


                // Receive the server's packet
                DatagramPacket received = new DatagramPacket(receiveData, receiveData.length);
                socket.setSoTimeout(5000); // TODO: TIMEOUT...take outside loop
                socket.receive(received);

                // Get the message opcode from server's packet.
                // MODE=ERROR because we will find out wich mode the presumed META packet has.
                meta_FTRapidPacket = new FTRapidPacket(received, FTRapidPacket.ERROR);

                // If we receive a META, stop the while loop. Check if MODE is correct.
                if (meta_FTRapidPacket.getOPCODE() == FTRapidPacket.META
                    && meta_FTRapidPacket.getTransferMODE() == this.MODE)
                {
                    timedOut = false;
                }
            }
            catch (SocketTimeoutException exception) {
                // If we don't get META, prepare to resend RQF.
                System.out.println("Server not responding to RQF.");
            } catch (IOException e) {
                if(this.socket.isClosed()) {
                    System.out.println("Receive Socket closed prematurely.");
                    return null;
                }
                else
                    e.printStackTrace();
            }
        }

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
        int prevSeqNum = 1;
        for (int index = 0; index < N_PACKETS; ++index) {
            // ACK packet - respecting sequence numbers.
            DatagramPacket ACK = FTRapidPacket.getACKPacket(ADDRESS, PORT, prevSeqNum);

            // First ACK is for the META packet (if index == 0)
            timedOut = true;
            while (timedOut) {
                byte[] buf = new byte[FTRapidPacket.BUFFER_SIZE];
                DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
                try{
                    // Send ACK for previous data packet.
                    this.socket.send(ACK);

                    // Wait and receive data packet.
                    this.socket.setSoTimeout(3000); // TODO: TIMEOUT...maybe put it outside loops
                    this.socket.receive(receivedPacket);

                    // Check and save packet.
                    FTRapidPacket ftRapidPacket = new FTRapidPacket(receivedPacket, this.MODE);
                    if(ftRapidPacket.getOPCODE() == FTRapidPacket.DATA
                            && ftRapidPacket.getSequenceNumber() != prevSeqNum)
                    {
                        allPackets.add(index, ftRapidPacket.getDataBytes().clone());
                        timedOut = false;
                    }
                }
                catch (SocketTimeoutException e){
                    System.out.println("Timeout while waiting for data packet. Resending ACK.");
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Change previous sequence number.
            prevSeqNum = prevSeqNum == 0? 1 : 0;
        }

        // Acknowledge last data packet - only once.
        // If the message doesn't get through, the sender will still be able to finish.
        DatagramPacket ACK = FTRapidPacket.getACKPacket(ADDRESS, PORT, prevSeqNum);
        try {
            this.socket.send(ACK);
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

                new FileOutputStream(this.filepath).write(fileBytes, 0, fileBytes.length);
            }
            catch (FileNotFoundException e){
                System.out.println("Receiver: " + this.filepath + " is not a valid filepath.");
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

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