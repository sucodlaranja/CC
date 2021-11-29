import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Handle each sync with another peer.
 *
 * */
public class SyncHandler implements Runnable{

    // TODO: Maybe create another class for this kind of stuff.
    private final int INIT = 1;
    private final int LISTENER_PORT = 8000;

    private final SyncInfo syncInfo;
    private DatagramSocket syncSocket;

    public SyncHandler(String filepath, InetAddress address){
        this.syncInfo = new SyncInfo(filepath, address);
    }

    public SyncInfo getInfo(){
        return this.syncInfo.clone();
    }

    public void closeSocket(){
        this.syncSocket.close();
    }

    // TODO: Create class to store protocol's helper methods.
    private byte[] ftRapidHeader(int header){
        /*
        * 0-ACK
        * 1-INIT
        * */
        // Hardcoded opcodes.
        final List<Integer> opcodes = new ArrayList<>(Arrays.asList(0,1));

        return opcodes.contains(header)? ByteBuffer.allocate(4).putInt(header).array() : null;
    }

    private DatagramPacket getRandomNumberPacket(){
        // Get INIT header
        byte[] header = this.ftRapidHeader(INIT);

        // Generate random integer byte array.
        byte[] rInt = ByteBuffer.allocate(4).putInt(new Random().nextInt()).array();

        // Filename to bytes.
        byte[] filename = this.syncInfo.getFilename().getBytes(StandardCharsets.UTF_8);

        assert header != null;
        byte[] rIntPacket = new byte[header.length + rInt.length + filename.length];

        // Copy header, random number and filename to the same byte array.
        System.arraycopy(header, 0, rIntPacket, 0, header.length);
        System.arraycopy(rInt, 0, rIntPacket, header.length, rInt.length);
        System.arraycopy(filename, 0, rIntPacket, header.length + rInt.length, filename.length);

        // Random number packet always goes to other peer listener on port X - X is known and pre-defined.
        return new DatagramPacket(rIntPacket, rIntPacket.length, this.syncInfo.getIpAddress(), LISTENER_PORT);
    }

    @Override
    public void run() {
        try {
            // Create socket and set SO timeout to 1s.
            this.syncSocket = new DatagramSocket();
            this.syncSocket.setSoTimeout(1000);

            // Create packet with random number => destination=listener_port
            DatagramPacket randomNumberPacket = this.getRandomNumberPacket();

            // Initiate sync - decide witch
            while(!this.syncSocket.isClosed()) {
                try {

                    // Send random number to other peer.
                    this.syncSocket.send(randomNumberPacket);

                    // TODO: Check if listener received random number from this ip with the same name of folder.

                }
                catch (SocketTimeoutException e){
                    // TIMEOUT: no problem...it will probably happen a few times in each start of sync.
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }



        }
        catch (SocketException e) {
            System.out.println("Failed to create socket in sync " + this.syncInfo.getId() + ".");
            e.printStackTrace();
        }
        finally {
            // Close socket
            this.closeSocket();

            System.out.println("Sync " + this.syncInfo.getId() + " terminated.");
        }
    }
}


