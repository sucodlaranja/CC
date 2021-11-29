
import org.jetbrains.annotations.NotNull;

import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


class UDPStopAndWaitClient{
    private static final int BUFFER_SIZE = 1024;
    private static final int PACKET_DATA_SIZE = 512;
    private static final int PACKET_SIZE = 514;
    private static final String PATH = "/home/rubensas/UM/3ano1sem/CC/PL/TP2/send_file_protocol/iteration_1_2/test/p1/test_file.txt";
    private static final String FILENAME = "test_file.txt";


    public static void run(InetAddress IPAddress, int port, @NotNull List<byte[]> packetsList, String meta) throws Exception {
        // Create a socket
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout( 1000 );

        // Send metadata to server
        byte[] controlMetaPac = {2, 0}; // Opcode=2, SeqNum=0
        byte[] metaBytes = meta.getBytes(StandardCharsets.UTF_8);

        byte[] metaPacBytes = new byte[controlMetaPac.length + metaBytes.length];
        System.arraycopy(controlMetaPac, 0, metaPacBytes, 0, controlMetaPac.length);
        System.arraycopy(metaBytes, 0, metaPacBytes, controlMetaPac.length, metaBytes.length);

        DatagramPacket metaPac = new DatagramPacket(metaPacBytes, metaPacBytes.length, IPAddress, port);
        socket.send(metaPac);


        // Wait for ACK from server.
        boolean timedOut = true;
        while(timedOut){
            try{
                // Create a byte array to receive ACK.
                byte[] receiveData = new byte[8];

                // Receive the server's packet
                DatagramPacket received = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(received);

                // Get the message opcode from server's packet.
                byte returnMessage = ByteBuffer.wrap(received.getData()).get();

                // If we receive an ack, stop the while loop
                if (returnMessage == 0){
                    timedOut = false;
                    port = received.getPort();
                }
                else
                    socket.send(metaPac);
            }
            catch (SocketTimeoutException exception) {
                // If we don't get an ack, prepare to resend metadata.
                System.out.println("Server not responding to metadata.");
                socket.send(metaPac);
            }
        }

        byte seqNum = 0;
        for (byte[] packData : packetsList) {
            timedOut = true;

            byte[] controlPack = {3, seqNum};
            byte[] pack = new byte[packData.length + controlPack.length];
            System.arraycopy(controlPack, 0, pack, 0, controlPack.length);
            System.arraycopy(packData, 0, pack, controlPack.length, packData.length);

            while (timedOut) {
                // Create a byte array for receiving data
                byte[] receiveData = new byte[BUFFER_SIZE];

                try {
                    // Send the UDP Packet to the server
                    DatagramPacket packet = new DatagramPacket(pack, pack.length, IPAddress, port);
                    socket.send(packet);

                    // Receive the server's packet
                    DatagramPacket received = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(received);

                    // Get the message from the server's packet
                    byte returnMessage = ByteBuffer.wrap(received.getData()).get();

                    // If we receive an ack, stop the while loop
                    if (returnMessage == 0)
                        timedOut = false;

                }
                catch (SocketTimeoutException exception) {
                    // If we don't get an ack, prepare to resend sequence number
                    System.out.println("Timeout. Packet will be resent.");
                }
            }

            if(seqNum == 1) seqNum = 0;
            else seqNum = 1;
        }

        socket.close();

    }

    public static void main(String[] args) throws Exception {

        // Assuming we're sending 512 bytes at a time, let's split the sendData byte array into nPackets. (IOException)
        byte[] fileData = Files.readAllBytes(Paths.get( PATH ));
        List<byte[]> packetsSplit = split(fileData);

        // Create Metadata: Filename, size of file, size of normal packet.
        String fileMetadata = FILENAME + "@" + fileData.length + "@" + PACKET_SIZE + "@";

        // Start transfer.
        run(InetAddress.getByName( "localhost" ), 8888, packetsSplit, fileMetadata);

    }

    public static List<byte[]> split(byte[] data) {

        int nPackets = (int) Math.ceil((double) data.length / PACKET_DATA_SIZE);
        List<byte[]> retList = new ArrayList<>(nPackets);

        int i;
        for (i = 0; i < nPackets - 1; i++) {
            retList.add(i, new byte[PACKET_DATA_SIZE]);
            System.arraycopy(data, i * PACKET_DATA_SIZE, retList.get(i), 0, retList.get(i).length);
        }

        int lastPacketSize = data.length - (nPackets-1) * PACKET_DATA_SIZE;
        retList.add(i, new byte[lastPacketSize]);
        System.arraycopy(data, i * PACKET_DATA_SIZE, retList.get(i), 0, retList.get(i).length);

        return retList;
    }
}