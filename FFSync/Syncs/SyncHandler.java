package Syncs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import FTRapid.FTRapidPacket;
import FTRapid.ReceiverSNW;
import FTRapid.SenderSNW;
import HTTP.HTTPServer;
import HistoryRecorder.TransferHistory;
import Listener.Listener;
import Logs.Guide;
import Logs.LogsManager;
import Logs.TransferLogs;
import Transfers.TransferHandler;

 /// Gestor de syncs (pré-guide)
/**
 * SyncHandler is the program heart. Everything starts and ends here.
 * A sync is managed from the early INIT/INIT_ACK stage until it starts the data exchange.
 * */
public class SyncHandler implements Runnable{

    private final SyncInfo syncInfo; ///< Information about this sync (id, active, ...)
    private DatagramSocket syncSocket; ///< SyncHandler socket.
    private final Integer ourRandom;    ///< Our randomly generated integer.
    private final DatagramPacket randomNumberPacket; ///< Random number packet.
    private boolean isBiggerNumber;    ///< ourRandom > peerRandom?
    private int handlerPort;     ///< Socket Port of syncHandler's peer.
    private final TransferHistory syncHistory; ///< Transfer history and stats about trasnfer.
    private LogsManager ourLogs; ///< Most recent logs.

    /**
     * SyncHandler is the program heart. Everything starts and ends here.
     * A sync is managed from the early INIT/INIT_ACK stage until it starts the data exchange.
     *
     * @param filepath Filepath of the sync (full path).
     * @param address Peer address.
     */
    public SyncHandler(String filepath, InetAddress address){
        // SyncInfo stores important information about this sync.
        this.syncInfo = new SyncInfo(filepath, address);

        // Create a packet with random number.
        this.ourRandom = new Random().nextInt();
        this.randomNumberPacket = FTRapidPacket.getINITPacket(this.getInfo().getIpAddress(), Listener.LISTENER_PORT, this.ourRandom, this.syncInfo.getFilename());

        // Instance of TransferHistory.
        this.syncHistory = new TransferHistory();
        this.ourLogs = null;
    }

    /// Our random number is the smaller.
    /**
     * Firstly, we calculate Logs.
     * loop:
     *      Sends INIT_ACK to peer listener
     *      Waits for ack in handler
     *
     * Send logs to peer handler port
     * Wait and receive guide
     *
     * @return Guide received from the other peer. If this object is null, then abort the sync.
     */
    private Guide inferiorRandomHandler() {
        // We're inferior.
        this.isBiggerNumber = false;

        // Calculate logs
        updateAndSaveLogs();

        // Abort sync if the LOGS can't be created.
        if(this.ourLogs == null)
            return null; // ABORT SYNC = null.

        // Create INIT_ACK packet (default port is LISTENER).
        DatagramPacket init_ack = FTRapidPacket.getINITACKPacket(this.getInfo().getIpAddress(), Listener.LISTENER_PORT, this.syncInfo.getFilename());

        FTRapidPacket ackPacket = FTRapidPacket.sendAndWaitLoop(this.syncSocket, init_ack, FTRapidPacket.ACK, FTRapidPacket.LOGS, FTRapidPacket.CONTROL_SEQ_NUM, false);
        if(ackPacket != null)
            this.handlerPort = ackPacket.getPort();
        else
            return null;

        // Serialize logs.
        byte[] logs = this.ourLogs.getBytes();

        // Send logs to peer handler port.
        SenderSNW senderSNW = new SenderSNW(this.syncSocket, this.syncInfo.getIpAddress(), this.handlerPort, logs, FTRapidPacket.LOGS);
        senderSNW.send();

        // Wait, receive and return guide.
        ReceiverSNW receiverSNW = new ReceiverSNW(this.syncSocket, FTRapidPacket.GUIDE, this.syncInfo.getIpAddress(), this.handlerPort);
        List<Object> list = receiverSNW.requestAndReceive();
        byte[] guideBytes = null;
        if (list != null) {
            guideBytes = (byte[]) list.get(0);
        }

        return guideBytes == null? null : new Guide(guideBytes); // ABORT SYNC = FALSE.
    }

    // Our random number is the bigger one.
    /**
     * If we're here it means we've found an INIT_ACK packet in the Listener request buffer.
     * We are the superior randm, so we go as follow:
     *  loop:
     *      Sends ack directly to peer handler (get port from INIT_ACK).
     *      Checks if peer is sending logs.
     *
     *  Receive Logs
     *  Calculate Guide
     *  Send Guide
     *
     * @return Guide created with the logs received from the other peer. If this object is null, then abort the sync.
     */
    private Guide superiorRandomHandler() {
        // We're superior.
        this.isBiggerNumber = true;

        // Acknowledge that we've received the INIT_ACK packet and receive LOGS.
        ReceiverSNW receiverSNW = new ReceiverSNW(this.syncSocket, FTRapidPacket.LOGS, this.getInfo().getIpAddress(), this.handlerPort);
        List<Object> list = receiverSNW.requestAndReceive();
        byte[] logsBytes = null;
        if (list != null)
            logsBytes = (byte[]) list.get(0);

        // Create LogsManager instance.
        if(logsBytes == null)
            return null;

        LogsManager beta = new LogsManager(logsBytes);

        // Update logs (and save them).
        updateAndSaveLogs();

        // Calculate Guide.
        Guide guide = new Guide(this.ourLogs.getLogs(), beta.getLogs());

        // Send guide.
        SenderSNW senderSNW = new SenderSNW(this.syncSocket, this.getInfo().getIpAddress(), this.handlerPort, guide.getBytes(), FTRapidPacket.GUIDE);
        senderSNW.send();

        return guide; // ABORT SYNC = null.
    }

    /**
     * This method executes a sync - syncs two folders in two different end-systems.
     * Send INIT packet to peer listener.
     * Checks pending requests from listener and chooses, which route to take.
     * After getting guide, proceeds to exchange files.
     */
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
                        this.syncSocket.send(FTRapidPacket.encode(this.randomNumberPacket));
                    }
                    catch (SocketException e){
                        System.out.println("SyncSocket closed.");
                    }
                    catch (IOException e) {
                        System.out.println("Failed to send INIT packet.");
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
        }

        // Check if socket is closed: something might have failed.
        if (!this.syncSocket.isClosed() && (guide != null) && (guide.getGuide().size() > 0)) {
            this.syncInfo.activate();

            TransferHandler transferHandler = new TransferHandler(5, guide, this.syncInfo.getFilepath(), this.syncSocket, this.syncInfo.getIpAddress(), this.handlerPort, this.isBiggerNumber);
            Set<TransferLogs> transfers = transferHandler.processTransfers();

            // Calculate logs and updating file.
            if(this.ourLogs != null) {
                try {
                    this.ourLogs.updateFileLogs();
                    this.syncHistory.updateLogs(this.ourLogs.getLogs());
                } catch (IOException ignored) {
                }
            }

            // Updating guide HTTP file.
            this.syncHistory.updateGuide(transfers);
            try {
                this.syncHistory.saveTransferHistory(HTTPServer.HTTP_FILEPATH + "/" + this.syncInfo.getFilename() + "-" + this.syncInfo.getId());
            } catch (IOException ignored) {
            }

            this.syncInfo.deactivate();
        }
    }

    /// Updates logs and saves them into HTTP file.
    private void updateAndSaveLogs() {
        try {
            this.ourLogs = new LogsManager(this.syncInfo.getFilepath());
            this.syncHistory.updateLogs(this.ourLogs.getLogs());
            this.syncHistory.saveTransferHistory(HTTPServer.HTTP_FILEPATH + "/" + this.syncInfo.getFilename() + "-" + this.syncInfo.getId());
        }
        catch (IOException e) {
            System.out.println("Failed to create logs from: " + this.syncInfo.getFilepath());
        }
    }

    /// Runs the syncOnce() method in loop, pausing for sometime after each run.
    @Override
    public void run() {
        try{
            // Create Socket.
            this.syncSocket = new DatagramSocket();

            // Sync folders with the other peer until the socket closes.
            while(!this.syncSocket.isClosed()){
                this.syncOnce();
                TimeUnit.SECONDS.sleep(5); // Sleep for 5 seconds.
            }

            // Termination message.
            System.err.println("Sync " + this.syncInfo.getId() + " terminated.");
        }
        catch (SocketException e) {
            System.out.println("Failed to create socket in sync " + this.syncInfo.getId() + ".");
        }
        catch (InterruptedException ignored) {
        }
    }

    /// Get sync information.
    public SyncInfo getInfo(){
        return this.syncInfo.clone();
    }

    /// Closes syncHandler socket. Used before exiting.
    public void closeSocket(){
        this.syncSocket.close();
    }

}
