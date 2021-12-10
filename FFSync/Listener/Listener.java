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

public class Listener implements Runnable{

    public static final int LISTENER_PORT = 8000;
    private static final int BUFFER_SIZE = 1024;

    private static Set<FTRapidPacket> rcvFTRapidPackets = null;
    private final DatagramSocket datagramSocket;

    public Listener() throws SocketException {
        this.datagramSocket = new DatagramSocket(LISTENER_PORT);
        if(rcvFTRapidPackets == null)
            rcvFTRapidPackets = new HashSet<>();
    }

    // TODO: CREATE MACROS FOR RETURN VALUES
    public static List<Integer> checkPendingRequests(String filename, InetAddress peerAddress, Integer ourRandom){
        // Return values range from -1 to 2 (at the moment).
        // 0:request_not_found, 1:our_random<peer_random, 2:INIT_ACK(our_random>peer_random), -1:randoms_are_equal
        // if our random is bigger than peer random, we simply ignore.
        ArrayList<Integer> return_value = new ArrayList<>();

        // Safety first
        if(rcvFTRapidPackets == null)
            rcvFTRapidPackets = new HashSet<>();

        // Calculate key
        Integer key = FTRapidPacket.calculateFTRapidPacketKey (filename, peerAddress);

        // Check if there is any pending request with the respective filename and from the address given above.
        for(FTRapidPacket packet : rcvFTRapidPackets){
            // Get packet - if keys are equal, filename and address should also be equal...
            if(packet.getKey().equals(key)
                && packet.getFilename().equals(filename)
                && packet.getPeerAddress().equals(peerAddress))
            {
                // Check different OPCODE.
                if(packet.getOPCODE() == FTRapidPacket.INIT){
                    // Check peer random number.
                    // If our is smaller, return 1.
                    // If our is bigger, return 0 (ignore).
                    // If random numbers are equal, return -1 (abort sync).
                    int r_int = ourRandom < packet.getRandomNumber()? 1 : (ourRandom > packet.getRandomNumber()? 0 : -1);
                    return_value.add(0, r_int);
                }
                else if(packet.getOPCODE() == FTRapidPacket.INIT_ACK){
                    // The other peer has already received our random number and our_random > peer_random.
                    return_value.add(0,2);
                    return_value.add(1, packet.getPort());
                }
            }
        }

        // TODO: In case there are no pending requests.
        if(return_value.isEmpty())
            return_value.add(0, 0);

        return return_value;
    }

    @Override
    public void run() {
        /*
         * Listener.Listener goal: listen to UDP requests.
         * UDP requests: Manage/Start FTRapid sync's.
         *
         * */

        // Receive (in UDP) requests and save them (not keeping repeated requests).
        while(!this.datagramSocket.isClosed()){
            byte[] rcvPacketBuf = new byte[BUFFER_SIZE];
            DatagramPacket rcvPacket = new DatagramPacket(rcvPacketBuf, rcvPacketBuf.length);
            try {
                this.datagramSocket.receive(rcvPacket);
            }
            catch (SocketException e){
                if(rcvFTRapidPackets != null)
                    rcvFTRapidPackets.clear();
            }
            catch (IOException e) {
                System.out.println("Failed to receive datagram packet in Listener.Listener.");
                e.printStackTrace();
            }

            // puts packet into set - keep repeated packets out
            if(!this.datagramSocket.isClosed())
                rcvFTRapidPackets.add(new FTRapidPacket(rcvPacket, FTRapidPacket.ERROR));
        }

        // Termination message
        System.err.println("Listener is offline.");
    }

    // TODO: CONCURRENCY?
    public void closeSocket(){
        this.datagramSocket.close();
    }
}
