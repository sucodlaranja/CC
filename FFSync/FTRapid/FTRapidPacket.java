package FTRapid;

import Listener.Listener;
import Syncs.SyncInfo;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Constructor receives datagram packet and transforms byte stream to readable and accessible information.
 *
 * */
public class FTRapidPacket {

    public static final int INVALID = -1;
    public static final int ACK = 0;
    public static final int INIT = 1;
    public static final int INIT_ACK = 2;


    private final Integer key;
    private final int OPCODE;
    private final int randomNumber;
    private final int port; // port of datagram packet.
    private final String filename;
    private final InetAddress PeerAddress;

    public FTRapidPacket(DatagramPacket rcvPacket) {
        // Parse data
        String[] data = ByteBuffer.wrap(rcvPacket.getData()).toString().split("@");

        int tempOpcode = Integer.parseInt(data[0]);
        if(tempOpcode == INIT && data.length > 2){
            // 1@random_int@filename@
            this.OPCODE = tempOpcode;
            this.randomNumber = Integer.parseInt(data[1]);
            this.filename = data[2];
            this.port = rcvPacket.getPort();
        }
        else if(tempOpcode == INIT_ACK && data.length > 1){
            // 2@filename@
            this.OPCODE = tempOpcode;
            this.randomNumber = 0;
            this.filename = data[1];
            this.port = rcvPacket.getPort();
        }
        else{
            System.out.println("OPCODE not found. Packet is invalid.");
            this.OPCODE = INVALID;
            this.randomNumber = 0;
            this.filename = "";
            this.port = -1;
        }

        this.PeerAddress = rcvPacket.getAddress();
        this.key = calculateFTRapidPacketKey(this.filename, this.PeerAddress);
    }

    public static Integer calculateFTRapidPacketKey(String filename, InetAddress address){
        return (filename + address.toString()).hashCode();
    }

    public Integer getKey() {
        return key;
    }
    public int getOPCODE() {
        return OPCODE;
    }
    public int getRandomNumber() {
        return randomNumber;
    }
    public int getPort() {
        return port;
    }
    public String getFilename() {
        return filename;
    }
    public InetAddress getPeerAddress() {
        return PeerAddress;
    }


    //  TODO: REDEFINE PACKET MESSAGES...DON'T USE '@'...
    // 1@random@filename@ => sends always to listener port and peer id saved in syncInfo.
    public static DatagramPacket getRandomNumberPacket(int random, SyncInfo syncInfo){
        String packetStr = FTRapidPacket.INIT + "@" + random + "@" + syncInfo.getFilename() + "@";
        byte[] packetBytes = packetStr.getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(packetBytes, packetBytes.length, syncInfo.getIpAddress(), Listener.LISTENER_PORT);
    }
    // 2@filename@ => always sends to listener port and peer id saved in syncInfo.
    public static DatagramPacket getInitAckPacket(SyncInfo syncInfo){
        String packetStr = FTRapidPacket.INIT_ACK + "@" + syncInfo.getFilename() + "@";
        byte[] packetBytes = packetStr.getBytes(StandardCharsets.UTF_8);

        return new DatagramPacket(packetBytes, packetBytes.length, syncInfo.getIpAddress(), Listener.LISTENER_PORT);
    }

    @Override
    public int hashCode() {
        return this.key;
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (o == null) return false;
        if (this.getClass() != o.getClass()) return false;
        FTRapidPacket ftrPacket = (FTRapidPacket) o;

        return this.key.equals(ftrPacket.key)
                && this.OPCODE == ftrPacket.OPCODE
                && this.randomNumber == ftrPacket.randomNumber
                && this.filename.equals(ftrPacket.filename);
    }

}
