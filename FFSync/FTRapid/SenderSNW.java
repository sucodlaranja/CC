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

    // Used to receive ACK packets.
    private final int ACK_BUFFER_SIZE = 8;

    // Filepath (if needed).
    private final String FILEPATH;

    // Data to be sent.
    private final byte[] dataToSend;

    // Data will be sent from this socket to the address bellow.
    private final DatagramSocket socket;
    private final InetAddress ADDRESS;
    private final int PORT;

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
    }

    // Send file to another peer, after sending META packet.
    // TODO: simplify code -> "while(timeout){}" piece of code is similar...
    public int send(){
        // Let's split the byte[] dataToSend into many packets (use packet size determined in FTRapidPacket class).
        List<byte[]> allPackets = split(this.dataToSend);

        // Create META packet.
        System.out.println("filesize=" + dataToSend.length + " : n_pack=" + allPackets.size()); // TODO: REMOVE
        System.out.println("filepath=" + FILEPATH);// TODO: REMOVE

        DatagramPacket metaPacket = FTRapidPacket.getMETAPacket(ADDRESS, PORT, this.MODE, dataToSend.length, this.FILEPATH);

        // Send META packet and wait for approval.
        boolean timedOut = true;
        while(timedOut) {
            try {
                // Send META packet.
                socket.send(metaPacket);

                // Create a byte array to receive ACK.
                byte[] receiveData = new byte[ACK_BUFFER_SIZE];

                // Receive the server's packet
                DatagramPacket received = new DatagramPacket(receiveData, receiveData.length);
                socket.setSoTimeout(3000); // TODO: TIMEOUT
                socket.receive(received);

                // Build FTRapidPacket.
                FTRapidPacket ftRapidPacket = new FTRapidPacket(received, this.MODE);

                // If we receive an ack, stop the while loop
                if (ftRapidPacket.getOPCODE() == FTRapidPacket.ACK && ftRapidPacket.getSequenceNumber() == 1) {
                    timedOut = false;
                } else
                    socket.send(metaPacket);
            }
            catch (SocketTimeoutException exception) {
                // If we don't get an ack, prepare to resend metadata.
                System.out.println("Server not responding to metadata.");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }


        int counter = 0;
        // Send data to the other peer.
        int seqNum = 0; // sequence number can only be 0/1.
        for (byte[] packData : allPackets) {
            int timeOutCounter = 5; // TODO: Is 5 timeouts limit a good number?

            // Create and send data packet.
            DatagramPacket dataPacket = FTRapidPacket.getDATAPacket(this.ADDRESS, this.PORT, seqNum, packData);
            timedOut = true;
            while (timedOut) {
                // Create a byte array for receiving data.
                byte[] receiveData = new byte[FTRapidPacket.BUFFER_SIZE];
                try {
                    // Send packet to peer.
                    socket.send(dataPacket);

                    // Receive the server's packet
                    DatagramPacket received = new DatagramPacket(receiveData, receiveData.length);
                    socket.setSoTimeout(2000); // TODO: TIMEOUT...
                    socket.receive(received);

                    // Build FTRapidPacket.
                    FTRapidPacket ftRapidPacket = new FTRapidPacket(received, this.MODE);

                    // If we receive an ack, stop the while loop
                    if (ftRapidPacket.getOPCODE() == FTRapidPacket.ACK && ftRapidPacket.getSequenceNumber() == seqNum)
                    {
                        timedOut = false;
                        System.out.println("Sended packet " + counter + "/" + (allPackets.size() - 1));// TODO: REMOVE
                        counter++;// TODO: REMOVE
                    }
                }
                catch (SocketTimeoutException exception) {
                    // If we don't get an ack, prepare to resend sequence number
                    System.out.println("Timeout. Packet will be resent.");
                    if(timeOutCounter > 0) timeOutCounter--;
                    else timedOut = false;
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }

            // Change sequence number.
            seqNum = seqNum == 0? 1 : 0;
        }

        // Close this socket only if in FILE mode - not going to be used again.
        if(this.MODE == FTRapidPacket.FILE)
            socket.close();


        System.out.println(this.FILEPATH + " was sent."); // TODO: REMOVE

        // ALL IS OK.
        return 0; // TODO: TEMPO TRANSFER + BITS/S - maybe with a record.
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
