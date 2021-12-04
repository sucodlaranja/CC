package Syncs;

import FTRapid.FTRapidPacket;
import Listener.Listener;
import Logs.LogsManager;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

/**
 * Handle each sync with another peer.
 *
 * */
public class SyncHandler implements Runnable{

    private final SyncInfo syncInfo;
    private final Integer ourRandom;
    private DatagramSocket syncSocket;

    public SyncHandler(String filepath, InetAddress address){
        this.syncInfo = new SyncInfo(filepath, address);
        this.ourRandom = new Random().nextInt();
    }

    public SyncInfo getInfo(){
        return this.syncInfo.clone();
    }

    public void closeSocket(){
        this.syncSocket.close();
    }


    // ourRandom < peerRandom
    // returns true if sync has to be aborted, false if sync can proceed.
    private boolean inferiorRandomHandler() {
        /*
        calculate logs
        loop:
            sends INIT_ACK to peer listener
            waits for ack in handler

        send logs to peer handler port
        wait and receive guide
        */

        // Calculate logs
        LogsManager logsManager = null;
        try {
            logsManager = new LogsManager(this.syncInfo.getFilepath());
        }
        catch (IOException e){
            System.out.println("Failed to create logs from: " + this.syncInfo.getFilepath());
            e.printStackTrace();
            return true; // ABORT SYNC = TRUE.
        }

        // Create INIT_ACK packet (default port is LISTENER).
        DatagramPacket init_ack = FTRapidPacket.getInitAckPacket(this.syncInfo);

        // Port number of the peer Syncs.SyncHandler.
        int peerHandlerPort = -1;

        // Sends INIT_ACK and waits for ACK.
        boolean ack_received = false;
        while(!ack_received){
            try {
                // Sending init_ack packet to peer listener.
                this.syncSocket.send(init_ack);

                // Create buffer and receive ack packet.
                byte[] ack_buffer = new byte[8]; // TODO: MAYBE USE MACRO TO REPRESENT ACK SIZE.
                DatagramPacket ack_packet = new DatagramPacket(ack_buffer, ack_buffer.length);

                // Set timeout to receive ACK packet. ACK has to arrive while we wait...
                this.syncSocket.setSoTimeout(3000);

                // Wait for ack (if timeout is reached, restart loop).
                this.syncSocket.receive(ack_packet);

                // TODO: Check if packet is valid
                if(new FTRapidPacket(ack_packet).getOPCODE() == FTRapidPacket.ACK){
                    ack_received = true;
                    peerHandlerPort = ack_packet.getPort();
                }
            }
            catch (SocketTimeoutException e){
                System.out.println("Waiting for ACK after INIT_ACK timeout.");
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }


        // TODO: Para enviar e receber precisamos de ter implementada a parte de send_receive_files_protocol.

        // Send logs to peer handler port.
        // TODO: <chamar metodo que envia cenas do protocolo...os logs tem de ser serializados ou algo assim>

        // Wait and receive guide
        // TODO: ver qual o formato do guia e guardar como variavel de instancia. para receber temos de chamar o metodo do protocolo que trata desta parte...

        return false; // ABORT SYNC = FALSE.
    }

    // ourRandom > peerRandom
    private void superiorRandomHandler(int peerHandlerPort) {
        // found INIT_ACK
        /*
        * loop:
        *   sends ack directly to peer handler (get port from INIT_ACK)
        *   checks if peer is sending logs
        *
        * receive logs
        * calculate guide
        * send guide
        * */



    }

    @Override
    public void run() {
        try {
            // TODO: ABORT SYNC IN CASE THE OTHER PEER ABORTED...CREATE TIMEOUT'S OR SOMETHING

            // Create socket
            this.syncSocket = new DatagramSocket();

            // Create packet with random number => destination=listener_port
            DatagramPacket randomNumberPacket = FTRapidPacket.getRandomNumberPacket(this.ourRandom, this.syncInfo);

            // Initiate sync
            boolean nextStep = false;
            boolean abortSync = false;
            while(!this.syncSocket.isClosed() && !nextStep) {

                // Check list of pending requests in Listener.Listener (INIT and INIT_ACK).
                List<Integer> reqStatus = Listener.checkPendingRequests(this.syncInfo.getFilename(), this.syncInfo.getIpAddress(), this.ourRandom);
                switch (reqStatus.get(0)) {
                    case 0 -> {
                        // Send random number to other peer.
                        try {
                            this.syncSocket.send(randomNumberPacket);
                        }
                        catch (IOException e) {
                            System.out.println("Failed to send INIT packet.");
                            e.printStackTrace();
                        }
                    }
                    case 1 -> {
                        // ourRandom < peerRandom: send INIT_ACK, calculate logs, send logs, wait for guide.
                        abortSync = this.inferiorRandomHandler();
                        nextStep = true;
                    }
                    case 2 -> {
                        // found INIT_ACK - wait for logs, get logs, calculate guide, send guide.
                        this.superiorRandomHandler(reqStatus.get(1));
                        nextStep = true;
                    }
                    default -> {
                        System.out.println("SYNC aborted, failed to reach consensus.");
                        this.closeSocket();
                    }
                }

                // ABORT SYNC IF NEEDED.
                if(abortSync)
                    this.closeSocket();

                // TODO: Insert some waiting time? Maybe only check pending requests if anything changed...
            }

            // Check if socket is closed: something might have failed.
            if(!this.syncSocket.isClosed()) {
                // TODO do jorge: Já temos o guião, e agora o jorge faz a magia.
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
