package Transfers;

import FTRapid.ReceiverSNW;
import FTRapid.SenderSNW;
import Logs.Guide;
import Logs.TransferLogs;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

/**
 * Handle the guide.
 *
 * */
public class TransferHandler {

    private final ThreadPool threadPool;
    private final FilesWaitingRequestPool filesWaitingRequestPool;

    // Used for processTransfers.
    private boolean checkProcessTransfers;
    private final Guide transfersGuide;
    private final String filepath;
    private final DatagramSocket syncSocket;
    private final InetAddress address;
    private final int handlerPort;
    private final boolean biggerNumber;


    // Basic Constructor
    public TransferHandler(int maxThreads, Guide transfersGuide, String filepath,
                           DatagramSocket syncSocket, InetAddress ipAddress, int handlerPort, boolean biggerNumber)
    {
        this.threadPool = new ThreadPool(maxThreads);
        this.filesWaitingRequestPool = new FilesWaitingRequestPool();
        this.checkProcessTransfers = false;
        this.transfersGuide = transfersGuide;
        this.filepath = filepath;
        this.syncSocket = syncSocket;
        this.address = ipAddress;
        this.handlerPort = handlerPort;
        this.biggerNumber = biggerNumber;
    }

    // Threads that listen for requests
    private class Listener implements Runnable{

        private final DatagramSocket syncSocket; // onde o listener vai ouvir
        private final InetAddress ipAddress;
        private final String filepath; // Add to filename to get the full path

        // Basic Constructor
        public Listener(DatagramSocket syncSocket,InetAddress ipAddress,String filepath){
            this.syncSocket = syncSocket;
            this.ipAddress = ipAddress;
            this.filepath = filepath;
        }

        // TODO magia do ruben para por este menino a ouvir em udp
        public String listen(DatagramSocket syncSocket){
            String filename = "";

            // Create a byte array to receive request.
            byte[] receiveData = new byte[8];

            // Receive the packet
            DatagramPacket received = new DatagramPacket(receiveData, receiveData.length);
            try {
                syncSocket.setSoTimeout(1000);
                syncSocket.receive(received);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return filename;
        }

        @Override
        public void run() {
            String filename = "";

            filesWaitingRequestPool.sleepIfEmpty(); // sleeps if there is no request to ear (set is empty)

            // ends when finish is true and the previous iteration did not give out a filename
            while (!filesWaitingRequestPool.getFinish() || !filename.equals("")){ // ends when
                filesWaitingRequestPool.sleepIfEmpty(); // sleeps if there is no request to ear (set is empty)

                // TODO Integrar a magia do ruben
                //listen(syncSocket);
                int communicateToPort = 1;
                filename = listen(syncSocket);

                // if the filename we read is on the set we create a thread to send it to the other person.
                if (filesWaitingRequestPool.removeUpcomingFiles(filename)){
                    Thread t = new Thread(new SendFile(communicateToPort,ipAddress,filepath + filename));
                    t.start();
                }

            }
            System.out.println("ACABEI DE LER MEUS PUTOS");
        }
    }

    // Threads that sends a file
    private class SendFile implements Runnable{
        // começa a enviar um file

        private final int sendToPort;
        private final InetAddress sendToIpAddress;
        private final String filepath;

        public SendFile(int sendToPort,InetAddress sendToIpAddress,String filepath){
            this.sendToPort = sendToPort;
            this.sendToIpAddress = sendToIpAddress;
            this.filepath = filepath;
        }

        @Override
        public void run() {
            // Send file.
            SenderSNW senderSNW = new SenderSNW(sendToIpAddress, sendToPort, filepath);
            senderSNW.send();

            threadPool.inc_dec_nMaxFiles(1);
        }
    }

    // Threads that requests and receives a file.
    private class ReceiveFile implements Runnable{
        // esabelece concecção e recebe o file

        private final int port;
        private final InetAddress address;
        private final String filepath;
        private final String filename;

        public ReceiveFile(int port, InetAddress address, String filepath, String filename){
            this.port = port;
            this.address = address;
            this.filepath = filepath;
            this.filename = filename;
        }

        @Override
        public void run() {
            System.out.println("Transfering file: " + filename + ".");
            ReceiverSNW receiverSNW = new ReceiverSNW(this.address, this.port, this.filepath, this.filename);
            receiverSNW.requestAndReceive();
            System.out.println(" Finished.");

            threadPool.inc_dec_nMaxFiles(1);
            //condition.signalAll();
        }
    }

    // Checks in what way to interpreter the queue
    // (It depends on if I am the one that creates the guide and what the guide says)
    private boolean doIRequest(boolean biggerNumber, boolean isSenderOrReceiver){
        if (biggerNumber) return isSenderOrReceiver;
        else return !isSenderOrReceiver;
    }

    // Main method of this class
    public void processTransfers(){
        // Get guide.
        Queue<TransferLogs> guide = this.transfersGuide.getGuide();

        // Execute processTransfers only once.
        if(checkProcessTransfers)
            return;

        // Creates a listener to ear requests
        Thread listener = new Thread(new Listener(syncSocket, address, filepath));
        listener.start();

        // Saves the size of the guide of transfers and the maximum of threads allowed per connection
        int size = guide.size();
        int max_files = threadPool.getNMaxFiles();
        System.out.println(max_files);

        // ends when there is no more files in the guide
        while (size != 0 ){

            //  Removes the first transfer in the guide
            TransferLogs oneTransfer = guide.poll();
            assert oneTransfer != null;

            // Verifies if I am the one to request the file
            if (doIRequest(biggerNumber,oneTransfer.isSender())) {
                // Starts the thread to request the file
                Thread t = new Thread(new ReceiveFile(handlerPort, address, filepath ,oneTransfer.getFileName()));
                t.start();
            }
            // Adds the file on the set of files that the other user wants to transfer
            else filesWaitingRequestPool.addUpcomingFiles(oneTransfer.getFileName());

            // Decrements the number of threads available
            threadPool.inc_dec_nMaxFiles(-1);
            size--;

            }


        // Waits for all threads to finish
        threadPool.waitForAllThreadsToFinish(max_files);
        filesWaitingRequestPool.setFinish();
        try {
            listener.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.checkProcessTransfers = true;
    }
}
