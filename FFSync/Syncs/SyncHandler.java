package Syncs;

import FTRapid.FTRapidPacket;
import FTRapid.ReceiverSNW;
import FTRapid.SenderSNW;
import Listener.Listener;
import Logs.Guide;
import Logs.LogsManager;
import Transfers.TransferHandler;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Random;


/**
 * Handle each sync with another peer.
 * */
public class SyncHandler implements Runnable{

    private final SyncInfo syncInfo;
    private DatagramSocket syncSocket;

    private final Integer ourRandom;
    private final DatagramPacket randomNumberPacket;

    private boolean isBiggerNumber; // ourRandom > peerRandom?
    private int handlerPort;        // Socket Port of syncHandler's peer.

    public SyncHandler(String filepath, InetAddress address){
        // SyncInfo stores important information about this sync.
        this.syncInfo = new SyncInfo(filepath, address);

        // Create a packet with random number.
        this.ourRandom = new Random().nextInt();
        this.randomNumberPacket = FTRapidPacket.getINITPacket(this.ourRandom, this.syncInfo);
    }

    // ourRandom < peerRandom
    // returns true if sync has to be aborted, false if sync can proceed.
    private Guide inferiorRandomHandler() {
        /*
        calculate logs
        loop:
            sends INIT_ACK to peer listener
            waits for ack in handler
        send logs to peer handler port
        wait and receive guide
        */

        // We're inferior.
        this.isBiggerNumber = false;

        // Calculate logs
        LogsManager logsManager = null;
        try {
            logsManager = new LogsManager(this.syncInfo.getFilepath());
        }
        catch (IOException e){
            System.out.println("Failed to create logs from: " + this.syncInfo.getFilepath());
            e.printStackTrace();
        }

        // Abort sync if the LOGS can't be created.
        if(logsManager == null)
            return null; // ABORT SYNC = null.

        // Create INIT_ACK packet (default port is LISTENER).
        DatagramPacket init_ack = FTRapidPacket.getINITACKPacket(this.syncInfo);

        // Sends INIT_ACK and waits for ACK.
        boolean ack_received = false;
        while(!ack_received){
            try {
                // Sending init_ack packet to peer listener.
                this.syncSocket.send(init_ack);

                // Create buffer and receive ack packet.
                byte[] ack_buffer = new byte[FTRapidPacket.ACK_BUFFER];
                DatagramPacket ack_packet = new DatagramPacket(ack_buffer, ack_buffer.length);

                // Set timeout to receive ACK packet. ACK has to arrive while we wait...
                this.syncSocket.setSoTimeout(4000);

                // Wait for ack (if timeout is reached, restart loop).
                this.syncSocket.receive(ack_packet);

                // Check packet is ACK and sequence number is CONTROL_SEQ_NUM
                FTRapidPacket ftRapidPacket = new FTRapidPacket(ack_packet, FTRapidPacket.ERROR);
                if(ftRapidPacket.getOPCODE() == FTRapidPacket.ACK
                    && ftRapidPacket.getSequenceNumber() == FTRapidPacket.CONTROL_SEQ_NUM)
                {
                    ack_received = true;
                    this.handlerPort = ack_packet.getPort();
                }
            }
            catch (SocketTimeoutException e){
                System.out.println("Waiting for ACK after INIT_ACK timeout.");
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

        // Serialize logs.
        byte[] logs = logsManager.getBytes();

        // Send logs to peer handler port.
        SenderSNW senderSNW = new SenderSNW(this.syncSocket, this.syncInfo.getIpAddress(), this.handlerPort, logs, FTRapidPacket.LOGS);
        senderSNW.send();

        // Wait, receive and return guide.
        ReceiverSNW receiverSNW = new ReceiverSNW(this.syncSocket, FTRapidPacket.GUIDE, this.syncInfo.getIpAddress(), this.handlerPort);
        byte[] guideBytes = receiverSNW.requestAndReceive();

        return guideBytes == null? null : new Guide(guideBytes); // ABORT SYNC = FALSE.
    }

    // ourRandom > peerRandom
    private Guide superiorRandomHandler() {
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

        // We're superior.
        this.isBiggerNumber = true;

        // Acknowledge that we've received the INIT_ACK packet and receive LOGS.
        ReceiverSNW receiverSNW = new ReceiverSNW(this.syncSocket, FTRapidPacket.LOGS, this.getInfo().getIpAddress(), this.handlerPort);
        byte[] logsBytes = receiverSNW.requestAndReceive();

        // Create LogsManager instance.
        LogsManager beta = new LogsManager(logsBytes);

        // Get our logs.
        // Calculate logs
        LogsManager alfa;
        try {
            alfa = new LogsManager(this.syncInfo.getFilepath());
        }
        catch (IOException e){
            System.out.println("Failed to create logs from: " + this.syncInfo.getFilepath());
            e.printStackTrace();
            return null;
        }

        // Calculate Guide.
        Guide guide = new Guide(alfa.getLogs(), beta.getLogs());

        // Send guide.
        SenderSNW senderSNW = new SenderSNW(this.syncSocket, this.getInfo().getIpAddress(), this.handlerPort, guide.getBytes(), FTRapidPacket.GUIDE);
        senderSNW.send();

        return guide; // ABORT SYNC = null.
    }

    private void syncOnce() {
        // Initiate sync
        boolean nextStep = false;
        Guide guide = null;
        while (!this.syncSocket.isClosed() && !nextStep) {

            // Check list of pending requests in Listener.
            List<Integer> reqStatus = Listener.checkPendingRequests(this.syncInfo.getFilename(), this.syncInfo.getIpAddress(), this.ourRandom);
            switch (reqStatus.get(0)) {
                case 0 -> {
                    // Send random number to other peer.
                    try {
                        this.syncSocket.send(this.randomNumberPacket);
                    }
                    catch (SocketException e){
                        System.out.println("SyncSocket closed.");
                    }
                    catch (IOException e) {
                        System.out.println("Failed to send INIT packet.");
                        e.printStackTrace();
                    }
                }
                // ourRandom < peerRandom: send INIT_ACK, calculate logs, send logs, wait for guide.
                case 1 -> guide = this.inferiorRandomHandler();

                case 2 -> {
                    // found INIT_ACK - wait for logs, get logs, calculate guide, send guide.
                    this.handlerPort = reqStatus.get(1);
                    guide = this.superiorRandomHandler();
                }
                default -> {
                    System.out.println("SYNC aborted, failed to reach consensus.");
                    this.closeSocket();
                }
            }

            // If guide can be created, go to next phase.
            if (guide != null)
                nextStep = true;
            else
                this.syncInfo.deactivate();

            // TODO: Insert some waiting time? Check pending requests only if anything changed...
            // TODO: Abort sync in case of no response for a given ammount of time.
        }

        // Check if socket is closed: something might have failed.
        if (!this.syncSocket.isClosed() && guide != null) {
            this.syncInfo.activate();
            TransferHandler transferHandler = new TransferHandler(5, guide, this.syncInfo.getFilepath(), this.syncSocket, this.syncInfo.getIpAddress(), this.handlerPort, this.isBiggerNumber);
            transferHandler.processTransfers();
        }

        // Close socket
        this.closeSocket();
    }

    @Override
    public void run() {
        try{
            // Create Socket.
            this.syncSocket = new DatagramSocket();

            // Sync folders with the other peer until the socket closes.
            while(!this.syncSocket.isClosed()){
                this.syncOnce();
                // TODO: write into syncInfo file?
            }

            // Termination message.
            System.err.println("Sync " + this.syncInfo.getId() + " terminated.");
        }
        catch (SocketException e) {
            System.out.println("Failed to create socket in sync " + this.syncInfo.getId() + ".");
            e.printStackTrace();
        }
    }

    public SyncInfo getInfo(){
        return this.syncInfo.clone();
    }
    public void closeSocket(){
        this.syncSocket.close();
    }
}