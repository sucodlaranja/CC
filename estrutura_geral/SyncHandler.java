import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
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

        assert header != null;
        byte[] rIntPacket = new byte[header.length + rInt.length];

        // Copy header and random number to the same byte array.
        System.arraycopy(header, 0, rIntPacket, 0, header.length);
        System.arraycopy(rInt, 0, rIntPacket, header.length, rInt.length);

        // Random number packet always goes to other peer listener on port X - X is known and pre-defined.
        return new DatagramPacket(rIntPacket, rIntPacket.length, this.syncInfo.getIpAddress(), LISTENER_PORT);
    }

    @Override
    public void run() {
        try {
            // Create socket and set SO timeout to 1s.
            this.syncSocket = new DatagramSocket();
            this.syncSocket.setSoTimeout(1000);

            // Create packet with random number - destination=listener_port
            DatagramPacket randomNumberPacket = this.getRandomNumberPacket();

            // Keep sending random number
            while(!this.syncSocket.isClosed()){
                this.syncSocket.send(randomNumberPacket);
            }

            // Close socket
            //this.closeSocket();
        }
        catch (SocketException e) {
            System.out.println("Failed to create socket in sync " + this.syncInfo.getId() + ".");
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        finally {
            System.out.println("Sync " + this.syncInfo.getId() + " terminated.");
        }
    }
}


