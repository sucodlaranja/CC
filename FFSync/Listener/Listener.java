package Listener;

import FTRapid.FTRapidPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class Listener implements Runnable{

    public static final int LISTENER_PORT = 8000;   // Default value for the socket port.

    // Feedback given to a specific syncHandler.
    public static final int REQUEST_NOT_FOUND = 0;  // Request not found or INIT packet received has inferior random.
    public static final int INFERIOR_RANDOM = 1;    // INIT packet received has a superior random.
    public static final int SUPERIOR_RANDOM = 2;    // Received a INIT_ACK packet meaning our random is superior.
    public static final int EQUAL_RANDOM = -1;      // INIT packet received has the same random number as ours.

    private static Set<FTRapidPacket> rcvFTRapidPackets = null;     // Received and different FTRapid packets.
    private final DatagramSocket datagramSocket;                    // Listener socket.
    private static final ReentrantLock lock = new ReentrantLock();  // Locks is used to control access to Set.

    public Listener() throws SocketException {
        rcvFTRapidPackets = new HashSet<>();
        this.datagramSocket = new DatagramSocket(LISTENER_PORT);
    }

    /**
     * Checks requests from other peers.
     * Sends feedback to the respective SyncHandler (locking Set required).
     * Possible feedback is explained above.
     * Packets found in rcvFTRapidPackets: INIT, INIT_ACK.
     * */
    public static List<Integer> checkPendingRequests(String filename, InetAddress peerAddress, Integer ourRandom){
        // Contains: feedback at i=0 and, if we receive INIT_ACK, peer handler port at i=1.
        ArrayList<Integer> return_value = new ArrayList<>();

        // Control access to the received packets Set.
        lock.lock();
        try {
            // Safety first (this is a static method).
            if (rcvFTRapidPackets == null)
                rcvFTRapidPackets = new HashSet<>();

            // The key will identify the packet that we are loocking for.
            Integer key = FTRapidPacket.calculateFTRapidPacketKey(filename, peerAddress);

            // Go through the received FTRapid packets.
            for (FTRapidPacket packet : rcvFTRapidPackets) {
                // Check if this is the packet we are searching (same key, filename and from the known peer address).
                if (packet.getKey().equals(key)
                        && packet.getFilename().equals(filename)
                        && packet.getPeerAddress().equals(peerAddress)) {
                    // INIT packet: the first step in our protocol communication (random numbers exchange).
                    if (packet.getOPCODE() == FTRapidPacket.INIT) {
                        // If our number is bigger, just ignore the packet.
                        int r_int = ourRandom < packet.getRandomNumber() ?
                                INFERIOR_RANDOM : (ourRandom > packet.getRandomNumber() ?
                                REQUEST_NOT_FOUND : EQUAL_RANDOM);

                        // Add feedback to the return list.
                        return_value.add(0, r_int);
                    }
                    // An INIT_ACK packet means that the other peer acknowledges that we have the superior random.
                    else if (packet.getOPCODE() == FTRapidPacket.INIT_ACK) {
                        // Add feedback to the return list (including the other peer handler port).
                        return_value.add(0, SUPERIOR_RANDOM);
                        return_value.add(1, packet.getPort());
                    }
                }
            }

            // In case there are no pending requests, send a REQUEST_NOT_FOUND.
            if (return_value.isEmpty())
                return_value.add(0, REQUEST_NOT_FOUND);
        }
        catch (Exception e){
            System.out.println("Checking pending requests failed.");
            e.printStackTrace();
        }
        finally {
            lock.unlock();
        }

        return return_value;
    }

    @Override
    public void run() {
        // Receive requests and save them (not keeping repeated requests).
        boolean close = true;
        while(!this.datagramSocket.isClosed() && close){
            // Received packet buffer.
            byte[] rcvPacketBuf = new byte[FTRapidPacket.BUFFER_SIZE];
            DatagramPacket rcvPacket = new DatagramPacket(rcvPacketBuf, rcvPacketBuf.length);
            try {
                this.datagramSocket.receive(FTRapidPacket.decode(rcvPacket, FTRapidPacket.DEFAULT_MUTUAL_SECRET));
            }
            // Socket was closed. Lock&Clean the house and leave.
            catch (SocketException e){
                lock.lock();
                if (rcvFTRapidPackets != null)
                    rcvFTRapidPackets.clear();
                close = false;
            }
            catch (IOException e) {
                System.out.println("Failed to receive datagram packet in Listener.");
                e.printStackTrace();
            }

            // Put packet into set.
            if(!this.datagramSocket.isClosed() && close){
                lock.lock();
                try {
                    rcvFTRapidPackets.add(new FTRapidPacket(rcvPacket, FTRapidPacket.ERROR));
                }
                catch (Exception e){
                    System.out.println("Adding packets to received packet set failed.");
                    e.printStackTrace();
                }
                finally {
                    lock.unlock();
                }
            }

        }

        // Termination message
        System.err.println("Listener is offline.");
    }

    // Close listener socket.
    public void closeSocket(){
        lock.lock();
        try{
            this.datagramSocket.close();
        }
        catch (Exception e){
            System.out.println("Failed to close listener socket.");
            e.printStackTrace();
        }
        finally {
            lock.unlock();
        }
    }

}
