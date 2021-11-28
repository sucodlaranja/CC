
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


class UDPStopAndWaitClient{

    private static final int PACKET_DATA_SIZE = 512;
    private static final int PACKET_SIZE = 514;
    private static final String PATH = "/home/rubensas/UM/3ano1sem/CC/PL/TP2/send_file_protocol/iteration_3/test/p1/test_file.txt";

    private static class SendFile implements Runnable {

        private static final int BUFFER_SIZE = 1024;

        private final InetAddress IPAddress;
        private int PORT;
        private final String metaStr;
        private final List<byte[]> packetsList;

        public SendFile(InetAddress IPAddress, int port, List<byte[]> packetsList, String meta) {
            this.IPAddress = IPAddress;
            this.PORT = port;
            this.metaStr = meta;
            this.packetsList = packetsList;
        }

        @Override
        public void run() {
            // Create a socket
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(1000);
            }
            catch (SocketException e){
                System.out.println("Failed to create socket.");
                e.printStackTrace();
            }
            finally {
                assert(socket != null);
            }

            // Send metadata to server
            byte[] controlMetaPac = {2, 0}; // Opcode=2, SeqNum=0
            byte[] metaBytes = metaStr.getBytes(StandardCharsets.UTF_8);

            byte[] metaPacBytes = new byte[controlMetaPac.length + metaBytes.length];
            System.arraycopy(controlMetaPac, 0, metaPacBytes, 0, controlMetaPac.length);
            System.arraycopy(metaBytes, 0, metaPacBytes, controlMetaPac.length, metaBytes.length);
            
            // Send metadata
            try {
                DatagramPacket metaPac = new DatagramPacket(metaPacBytes, metaPacBytes.length, IPAddress, PORT);
                socket.send(metaPac);

                // Wait for ACK from server.
                boolean timedOut = true;
                while (timedOut) {
                    try {
                        // Create a byte array to receive ACK.
                        byte[] receiveData = new byte[8];

                        // Receive the server's packet
                        DatagramPacket received = new DatagramPacket(receiveData, receiveData.length);
                        socket.receive(received);

                        // Get the message opcode from server's packet.
                        byte returnMessage = ByteBuffer.wrap(received.getData()).get();

                        // If we receive an ack, stop the while loop
                        if (returnMessage == 0) {
                            timedOut = false;
                            PORT = received.getPort();
                        } else
                            socket.send(metaPac);
                    } catch (SocketTimeoutException exception) {
                        // If we don't get an ack, prepare to resend metadata.
                        System.out.println("Server not responding to metadata.");
                        socket.send(metaPac);
                    }
                }

            }
            catch (IOException e){
                System.out.println("Failed to send metadata.");
                e.printStackTrace();
            }




            /*
            final byte WINDOW_SIZE = 4;
            for (byte[] packData : packetsList) {
                boolean timedOut = true;

                // Create and send WINDOW_SIZE packet's.
                for(int currentPacketNumber = 0; currentPacketNumber < WINDOW_SIZE; ++currentPacketNumber){
                    // Create packet.
                    byte seqNum = (byte) (currentPacketNumber % WINDOW_SIZE);
                    byte[] controlPack = {3, seqNum};
                    byte[] pack = new byte[packData.length + controlPack.length];
                    System.arraycopy(controlPack, 0, pack, 0, controlPack.length);
                    System.arraycopy(packData, 0, pack, controlPack.length, packData.length);

                    // Send packet
                    try{
                        // Send the UDP Packet to the server
                        DatagramPacket packet = new DatagramPacket(pack, pack.length, IPAddress, PORT);
                        socket.send(packet);


                    }
                    catch (SocketTimeoutException exception) {
                        // If we don't get an ack, prepare to resend sequence number
                        System.out.println("Timeout. Packet will be resent.");
                    }
                    catch (IOException e) {
                        System.out.println("Failed to send data.");
                        e.printStackTrace();
                    }
                }


                while (timedOut) {
                    // Create a byte array for receiving data
                    byte[] receiveData = new byte[BUFFER_SIZE];

                    try {


                        // Receive the server's packet
                        DatagramPacket received = new DatagramPacket(receiveData, receiveData.length);
                        socket.receive(received);

                        // Get the message from the server's packet
                        byte returnMessage = ByteBuffer.wrap(received.getData()).get();

                        // If we receive an ack, stop the while loop
                        if (returnMessage == 0)
                            timedOut = false;

                    }

                }

            }
             */
            socket.close();

        }

    }

    private static final String FILENAME = "test_file.txt";

    public static void main(String[] args) throws Exception {

        // Assuming we're sending 512 bytes at a time, let's split the sendData byte array into nPackets. (IOException)
        byte[] fileData = Files.readAllBytes(Paths.get( PATH ));
        List<byte[]> packetsSplit = split(fileData);

        // Create Metadata: Filename, size of file, size of normal packet.
        String fileMetadata = FILENAME + "@" + fileData.length + "@" + PACKET_SIZE + "@";

        // Start transfer.
        Thread thread = new Thread(new SendFile(InetAddress.getByName( "localhost" ), 8888, packetsSplit, fileMetadata));
        thread.start();
        thread.join();
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