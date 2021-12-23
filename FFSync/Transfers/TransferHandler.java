package Transfers;

import FTRapid.FTRapidPacket;
import FTRapid.ReceiverSNW;
import FTRapid.SenderSNW;
import Logs.Guide;
import Logs.TransferLogs;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

/// This class will create all the threads that send and receive files.
/**
 * This class will create and manage all the threads. \n
 * Will use method \ref processTransfers as its core.
 *
 * */
public class TransferHandler {

    /// Shared information between TransferHandler and the other threads.Will also store info.
    private final ThreadPool threadPool;
    /// Shared information between TransferHandler and the Listener it creates.
    private final FilesWaitingRequestPool filesWaitingRequestPool;

    /// Won't allow system to run more that once.
    private boolean checkProcessTransfers;
    /// The guide this class will follow.
    private final Guide transfersGuide;
    /// The folder path of this sync.
    private final String folderPath;
    /// Socket where the Listener will listen.
    private final DatagramSocket syncSocket;
    /// Ip to communicate to
    private final InetAddress address;
    /// Port to communicate to
    private final int handlerPort;
    /// If it has the bigger number of the protocol. If it created the guide or not.
    private final boolean biggerNumber;


    /// Basic Constructor
    public TransferHandler(int maxThreads, Guide transfersGuide, String filepath,
                           DatagramSocket syncSocket, InetAddress ipAddress, int handlerPort, boolean biggerNumber)
    {
        this.threadPool = new ThreadPool(maxThreads);
        this.filesWaitingRequestPool = new FilesWaitingRequestPool();
        this.checkProcessTransfers = false;
        this.transfersGuide = transfersGuide;
        this.folderPath = filepath;
        this.syncSocket = syncSocket;
        this.address = ipAddress;
        this.handlerPort = handlerPort;
        this.biggerNumber = biggerNumber;
    }

    /**
     * Private class only used by \ref TransferHandler. \n
     * This class will listen to requests and create the appropriate threads to respond. \n
     * If there is no more requests that it is expecting to receive, it waits for a signal whit method \ref sleepIfEmpty.
     */
    private class Listener implements Runnable{

        /// Socket where it will listen
        private final DatagramSocket syncSocket;
        /// Path of the folder of this sync
        private final String folderPath;

        // Basic Constructor
        public Listener(DatagramSocket syncSocket,String filepath){
            this.syncSocket = syncSocket;
            this.folderPath = filepath;
        }

        /// This method will listen to 1 request and end.
        public FTRapidPacket listen(){

            // Create a byte array to receive request.
            byte[] receiveData = new byte[FTRapidPacket.BUFFER_SIZE];

            // Receive the packet
            DatagramPacket received = new DatagramPacket(receiveData, receiveData.length);
            try {
                syncSocket.setSoTimeout(5000);
                syncSocket.receive(FTRapidPacket.decode(received));
            } catch (IOException ignored){

            }

            return new FTRapidPacket(received, FTRapidPacket.INVALID);
        }

        /**
         * Ends when \ref getFinish is true and the previous iteration did not give out a filename. \n
         * Sleeps if there is no request to ear (set is empty). \n
         * If the filename we read is on the set we create a thread to send it to the peer.
         */
        @Override
        public void run() {
            String filename;

            // ends when finish is true and the previous iteration did not give out a filename
            do{
                filesWaitingRequestPool.sleepIfEmpty(); // sleeps if there is no request to ear (set is empty)

                FTRapidPacket packet = listen();
                int communicateToPort = packet.getPort();
                InetAddress ipAddress = packet.getPeerAddress();
                filename = packet.getFilename();

                // if the filename we read is on the set we create a thread to send it to the other person.
                if (filesWaitingRequestPool.removeUpcomingFiles(filename)){
                    String completeFilepath = getCompleteFilepath(folderPath, filename);
                    Thread t = new Thread(new SendFile(communicateToPort, ipAddress, completeFilepath));
                    t.start();
                }

            } while(!filesWaitingRequestPool.getFinish() || !filename.equals(""));
        }
    }

    /**
     * Private class only used by \ref TransferHandler. \n
     * This class will send a file.
     */
    private class SendFile implements Runnable{

        /// Port to send to.
        private final int sendToPort;
        /// IP to send to.
        private final InetAddress sendToIpAddress;
        /// Complete filepath
        private final String filepath; // this is the complete filepath

        /// Basic constructor
        public SendFile(int sendToPort,InetAddress sendToIpAddress,String filepath){
            this.sendToPort = sendToPort;
            this.sendToIpAddress = sendToIpAddress;
            this.filepath = filepath;
        }

        /**
         * Send a file with the help of \ref SenderSNW. \n
         * Adds info of the sent file to the \ref ThreadPool. \n
         * Also communicates that 1 more thread is allowed
          */
        @Override
        public void run() {
            // Send file.
            SenderSNW senderSNW = new SenderSNW(sendToIpAddress, sendToPort, filepath);
            TransferLogs transferLogs = senderSNW.send();
            threadPool.addTransferLogs(transferLogs);
            threadPool.inc_dec_nMaxFiles(1);
        }
    }

    /**
     * Private class only used by \ref TransferHandler. \n
     * This class will request and receive a file.
     */
    private class ReceiveFile implements Runnable{

        /// Port to send request.
        private final int port;
        /// IP to send request.
        private final InetAddress address;
        /// The folder path of this sync.
        private final String folderPath;
        /// Filename we want to request.
        private final String filename;

        /// Basic Constructor
        public ReceiveFile(int port, InetAddress address, String filepath, String filename){
            this.port = port;
            this.address = address;
            this.folderPath = filepath;
            this.filename = filename;
        }

        /**
         * Request and Receive a file with the help of \ref receiverSNW. \n
         * Adds info of the received file to the \ref ThreadPool. \n
         * Also communicates that 1 more thread is allowed.
         */
        @Override
        public void run() {
            ReceiverSNW receiverSNW = new ReceiverSNW(this.address, this.port, getCompleteFilepath(folderPath, filename), this.filename);
            List<Object> list = receiverSNW.requestAndReceive();
            if (list != null) threadPool.addTransferLogs((TransferLogs) list.get(1));
            threadPool.inc_dec_nMaxFiles(1);
        }
    }

    /// Checks in what way to interpreter the queue.Depends on if I am the one that creates the guide what it orders.
    private boolean doIRequest(boolean biggerNumber, boolean isSenderOrReceiver){
        if (biggerNumber) return isSenderOrReceiver;
        else return !isSenderOrReceiver;
    }

    /// Creates a correct filepath (in terms of format).
    private String getCompleteFilepath(String filepath, String filename){
        return filepath.charAt(filepath.length() - 1) == '/' ? filepath + filename : filepath + "/" + filename;
    }


    /**
     * Main method of this class. \n
     * It can only run once. \n
     * Creates a listener to listen to requests. \n
     * Will follow a guide when there is space for more threads. \n
     * Either creates a thread to Request or adds files to wait for request. \n
     * If there is no more space for threads, it waits for 1 to end with the help of \ref inc_dec_nMaxFiles. \n
     * In the end waits for all threads to finish whit \ref waitForAllThreadsToFinish. \n
     * Then signals the listener to end and waits for him to end.
     *
     * @return The set whit the Every transfer info(Velocity,Time).
     */
    public Set<TransferLogs> processTransfers(){
        // Get guide.
        Queue<TransferLogs> guide = this.transfersGuide.getGuide();

        // Execute processTransfers only once.
        if(checkProcessTransfers)
            return threadPool.getTransferLogs();

        // Creates a listener to listen to requests.
        Thread listener = new Thread(new Listener(syncSocket, folderPath));
        listener.start();

        // Saves the size of the guide of transfers and the maximum of threads allowed per connection
        int size = guide.size();
        int max_files = threadPool.getNMaxFiles();

        // ends when there are no more files in the guide
        while (size > 0 ){

            //  Removes the first transfer in the guide
            TransferLogs oneTransfer = guide.poll();
            assert oneTransfer != null;

            // Verifies if I am the one to request the file
            if (doIRequest(biggerNumber,oneTransfer.sender())) {
                // Starts the thread to request the file
                Thread t = new Thread(new ReceiveFile(handlerPort, address, folderPath,oneTransfer.fileName()));
                t.start();
            }
            // Adds the file on the set of files that the other user wants to transfer
            else {
                filesWaitingRequestPool.addUpcomingFiles(oneTransfer.fileName());
            }

            // Decrements the number of threads available
            threadPool.inc_dec_nMaxFiles(-1);
            size--;
        }

        // Waits for all threads to finish
        threadPool.waitForAllThreadsToFinish(max_files);

        filesWaitingRequestPool.setFinish();
        try {
            listener.join();
        }
        catch (InterruptedException ignored) {
        }

        this.checkProcessTransfers = true;
        return threadPool.getTransferLogs();
    }
}