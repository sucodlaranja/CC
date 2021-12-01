import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handle the guide.
 *
 * */
public class TransferHandler {

    private int nMaxFiles;
    private final Set<String> upcomingFiles;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public TransferHandler(int nMaxFiles){
        this.nMaxFiles = nMaxFiles;
        upcomingFiles = new HashSet<>();
    }

    public int getNMaxFiles(){ return nMaxFiles; }

    public void inc_dec_nMaxFiles(int incOrDec){
        lock.lock();
        try {
            nMaxFiles += incOrDec;
        }
        finally {
            lock.unlock();
        }
    }

    public void addUpcomingFiles(String filename){
        lock.lock();
        try {
            upcomingFiles.add(filename);
        }
        finally {
            lock.unlock();
        }
    }

    public boolean removeUpcomingFiles(String filename){
        lock.lock();
        boolean exists;
        try {
            exists = upcomingFiles.remove(filename);
        }
        finally {
            lock.unlock();
        }
        return exists;
    }

    public class Listener implements Runnable{

        private final DatagramSocket syncSocket; // onde o listener vai ouvir
        private final InetAddress ipAddress;
        private final String filepath;

        public Listener(DatagramSocket syncSocket,InetAddress ipAddress,String filepath){
            this.syncSocket = syncSocket;
            this.ipAddress = ipAddress;
            this.filepath = filepath;
        }
        @Override
        public void run() {
            // está sempre a ouvir
            while (true){
                String filename = "";
                int communicateToPort = 1;

                //ouvir udp

                if (removeUpcomingFiles(filename)){
                    Thread t = new Thread(new SendFile(communicateToPort,ipAddress,filepath + filename));
                    t.start();
                }
            }
        }
    }

    public class SendFile implements Runnable{
        // começa a enviar um file

        private final int sendToPort;
        private final InetAddress sendToIpAddress;
        private final String fileName;

        public SendFile(int sendToPort,InetAddress sendToIpAddress,String filepath){
            this.sendToPort = sendToPort;
            this.sendToIpAddress = sendToIpAddress;
            this.fileName = filepath;
        }

        @Override
        public void run() {
            //Sender_UDP s = new Sender_UDP(sendToPort,sendToIpAddress,filepath);
            inc_dec_nMaxFiles(1);
            condition.signal();
        }
    }

    public class ReceiveFile implements Runnable{
        // esabelece concecção e recebe o file

        private final int requestToPort;
        private final InetAddress requestToIpAddress;
        private final String fileName;

        public ReceiveFile(int requestToPort,InetAddress requestToIpAddress,String fileName){
            this.requestToPort = requestToPort;
            this.requestToIpAddress = requestToIpAddress;
            this.fileName = fileName;
        }

        @Override
        public void run() {
            //Receiver_UDP c = new Receiver_UDP(requestToPort,requestToIpAddress,fileName);
            inc_dec_nMaxFiles(1);
            condition.signal();
        }
    }

    public boolean doIRequest(boolean biggerNumber,boolean isSenderOrReceiver){
        if (biggerNumber) return !isSenderOrReceiver;
        else return isSenderOrReceiver;
    }

    public void processTransfers(Queue<TransferLogs> listOfTransfers,String filepath, DatagramSocket syncSocket,
                                 InetAddress ipAddress,int sendToPortHandler,boolean biggerNumber) {

        Thread listener = new Thread(new Listener(syncSocket,ipAddress,filepath));
        listener.start();
        int size = listOfTransfers.size();
        int max_files = nMaxFiles;

        while (size != 0 || getNMaxFiles() != max_files){

            while (getNMaxFiles() != 0 && size != 0) {
                TransferLogs oneTransfer = listOfTransfers.poll();

                if (doIRequest(biggerNumber,oneTransfer.isSenderOrReceiver())) {
                    Thread t = new Thread(new ReceiveFile(sendToPortHandler,ipAddress,oneTransfer.getFileName()));
                    t.start();
                } else {
                    addUpcomingFiles(oneTransfer.getFileName());
                }

                inc_dec_nMaxFiles(-1);
                size--;
            }

            try {
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
