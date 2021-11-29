
import java.io.*;
import java.net.*;
import java.util.*;



class TransferFile implements Runnable{

    private final InetAddress CLIENT_ADDRESS;
    private final int PORT;
    private final int N_PACKETS;
    private final int PACKET_DATA_SIZE;
    private final int LAST_PACKET_DATA_SIZE;
    private final List <byte[]> allPackets;
    private DatagramSocket threadSocket;

    private static final byte[] ACK = {0};
    private static final byte DATA = 3;

    private static final String FILEPATH= "/home/rubensas/UM/3ano1sem/CC/PL/TP2/send_file_protocol/iteration_1_2/test/p2/received.txt";

    public TransferFile(int filesize, int packetSize, InetAddress address, int port) {
        this.PACKET_DATA_SIZE = packetSize - 2;
        this.CLIENT_ADDRESS = address;
        this.PORT = port;
        this.N_PACKETS = (int) Math.ceil((double) filesize / PACKET_DATA_SIZE);
        this.LAST_PACKET_DATA_SIZE = filesize - (N_PACKETS - 1) * PACKET_DATA_SIZE;
        this.allPackets = new ArrayList<>(N_PACKETS);
    }

    @Override
    public void run() {

        // Create thread socket and answer client with ACK.
        this.threadSocket = null;
        try {
            // Create thread socket.
            threadSocket = new DatagramSocket();
            threadSocket.setSoTimeout(1000);

            // Send ACK packet to the client signaling that we've received metadata.
            DatagramPacket packet = new DatagramPacket( ACK, ACK.length, CLIENT_ADDRESS, PORT );
            threadSocket.send( packet );

        }
        catch (IOException e){
            System.out.println("Thread Socket failed to be created.");
            e.printStackTrace();
        }
        finally {
            assert(threadSocket != null);
        }

        // Receive client data.
        try{
            byte prevNSeq = 1;
            int index;
            for(index = 0; index < this.N_PACKETS - 1; ++index)
                prevNSeq = receiveAndACK(index, prevNSeq);

            receiveAndACK(index, prevNSeq);
        }
        catch (IOException e){
            System.out.println("Failed to receive packet.");
            e.printStackTrace();
        }

        try{
            // Colapsar o array de packets num array de bytes.
            byte[] collapsedPackets = collapse(allPackets);
            try (FileOutputStream stream = new FileOutputStream(FILEPATH)) {
                stream.write(collapsedPackets, 0, collapsedPackets.length);
            }
        }
        catch (IOException e){
            System.out.println("File not found.");
            e.printStackTrace();
        }
        finally {
            allPackets.clear();
        }

    }

    private byte receiveAndACK(int index, byte prevNSeq) throws IOException {
        byte[] receivedData = new byte[PACKET_DATA_SIZE + 2];

        // Receive client's packet.
        DatagramPacket received = new DatagramPacket(receivedData, receivedData.length);
        threadSocket.receive(received);

        // Check opcode and seqNumber
        if((receivedData.length > 1)
                && (receivedData[0] == DATA)
                && (receivedData[1] != prevNSeq))
        {
            byte[] data = new byte[PACKET_DATA_SIZE]; // remove first two bytes
            System.arraycopy(receivedData, 2, data, 0, PACKET_DATA_SIZE);

            // Store packet content
            this.allPackets.add(index, data);

            // ACK to client - assume it arrives to destination.
            DatagramPacket packet = new DatagramPacket( ACK, ACK.length, CLIENT_ADDRESS, PORT );
            threadSocket.send( packet );
        }
        return receivedData[1];
    }

    private byte[] collapse(List<byte[]> packetsSplit) {
        byte[] ret = new byte[(packetsSplit.size() - 1) * PACKET_DATA_SIZE + LAST_PACKET_DATA_SIZE];

        int i;
        for(i = 0; i < packetsSplit.size() - 1; ++i)
            System.arraycopy(packetsSplit.get(i), 0, ret, i * PACKET_DATA_SIZE, PACKET_DATA_SIZE);


        int offset = i * PACKET_DATA_SIZE;
        byte[] buf = packetsSplit.get(i);
        for(int k = offset, j = 0; k < offset + LAST_PACKET_DATA_SIZE; ++k, j++)
            ret[k] = buf[j];

        return ret;
    }

}


class UDPStopAndWaitServer{
    private static final int BUFFER_SIZE = 1024;
    private static final int SERVER_PORT = 8888;
    private static final int MAX_THREADS = 8;

    private static final byte META = 2;

    public static void main(String[] args) throws IOException {
        // Create a server socket
        DatagramSocket serverSocket = new DatagramSocket( SERVER_PORT );

        // Create threads to handle the different transfers
        Thread[] threads = new Thread[MAX_THREADS];

        int USED_THREADS = 0;
        // Check for metadata
        while(true){
            // Received packet
            byte[] buf = new byte[ BUFFER_SIZE ];

            // Get the received packet
            DatagramPacket received = new DatagramPacket( buf, buf.length );
            serverSocket.receive( received );

            // Check opcode for META.
            byte[] packetBytes = received.getData();
            if(packetBytes[0] == META){
                byte[] metadataBytes = new byte[packetBytes.length - 2]; // remove first two bytes
                System.arraycopy(packetBytes, 2, metadataBytes, 0, packetBytes.length - 2);

                // Analyse metadata: filename, filesize, packetsize
                List<String> metadata = new ArrayList<>(Arrays.asList(new String(metadataBytes).split("@", 4)));
                metadata.remove(metadata.size() - 1);

                // Validate metadata.
                if(metadataValidation(metadata) && (USED_THREADS < MAX_THREADS)){

                    // Check for valid index.
                    int idx = 0;
                    while(threads[idx] != null) {
                        if (threads[idx].isAlive()) {
                            idx++;
                        } else {
                            USED_THREADS--;
                            threads[idx] = null;
                        }
                    }

                    // Create new thread to start transfer.
                    threads[idx] = new Thread(new TransferFile(
                            Integer.parseInt(metadata.get(1)),
                            Integer.parseInt(metadata.get(2)),
                            received.getAddress(),
                            received.getPort()
                    ));
                    threads[idx].start();
                    USED_THREADS++;
                }
            }

        }
    }

    private static boolean metadataValidation(List<String> metadata) {
        // In order to be valid: [filename, filesize, packetsize]
        if(metadata.size() == 3){
            try {
                Integer.parseInt(metadata.get(1));
                Integer.parseInt(metadata.get(2));
                return true;
            }
            catch (NumberFormatException e)
            {
                return false;
            }

            // Check filename...

        }
        else
            return false;
    }

}