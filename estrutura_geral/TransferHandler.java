import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handle the guide.
 *
 * */
public class TransferHandler {

    private final ThreadPool threadPool;
    private final FilesWaitingRequestPool filesWaitingRequestPool;

    public TransferHandler(int nMaxFiles){
        threadPool = new ThreadPool(nMaxFiles);
        filesWaitingRequestPool = new FilesWaitingRequestPool();
    }

    public static class ThreadPool{
        private int nMaxFiles;
        private final ReentrantLock lock;
        private final Condition condition;

        public ThreadPool(int nMaxFiles) {
            this.nMaxFiles = nMaxFiles;
            this.lock = new ReentrantLock();
            this.condition = lock.newCondition();
        }

        public boolean compareMaxFiles(int max_files) {
            boolean response;
            lock.lock();
            try {
                if (max_files == nMaxFiles) response = false;
                else {
                    try {
                        condition.await();
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    response = true;
                }
            }
            finally {
                lock.unlock();
            }
            return response;
        }

        public int getNMaxFiles(){
           int number;
           lock.lock();
           try {
               number = nMaxFiles;
           }
           finally {
               lock.unlock();
           }
           return number;
        }

        public void inc_dec_nMaxFiles(int incOrDec){
            lock.lock();
            try {
                nMaxFiles += incOrDec;
                System.out.println("nMaxFiles -> " + (nMaxFiles - incOrDec) + " -> " + nMaxFiles);
                if (incOrDec == 1) condition.signalAll();
                else if (incOrDec == -1 && nMaxFiles == 0) {
                    System.out.println("Adormeci");
                    condition.await();
                    System.out.println("Acordei");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }

    }

    public static class FilesWaitingRequestPool{
        private boolean finish;
        private final Set<String> upcomingFiles;
        private final ReentrantLock lock;
        private final Condition condition;

        public FilesWaitingRequestPool() {
            finish = false;
            upcomingFiles = new HashSet<>();
            lock = new ReentrantLock();
            condition = lock.newCondition();
        }

        public boolean getFinish() {
            boolean response;
            lock.lock();
            try {
                response = finish;
            }
            finally {
                lock.unlock();
            }
            return response;
        }

        public void setFinish() {
            lock.lock();
            try {
                finish = true;
                condition.signalAll();
            }
            finally {
                lock.unlock();
            }
        }

        public void sleepIfEmpty(){
            lock.lock();
            try {
                if (upcomingFiles.isEmpty()) condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }

        public void addUpcomingFiles(String fileName){
            lock.lock();
            try {
                boolean awake = upcomingFiles.isEmpty();
                upcomingFiles.add(fileName);
                if (awake) condition.signalAll();
            }
            finally {
                lock.unlock();
            }
        }

        public boolean removeUpcomingFiles(String fileName){
            boolean response;
            lock.lock();
            try {
                response = upcomingFiles.remove(fileName);
            }
            finally {
                lock.unlock();
            }
            return response;
        }

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
            // está sempre a ouvir

            String filename = "";

            filesWaitingRequestPool.sleepIfEmpty();
            while (!filesWaitingRequestPool.getFinish() || !filename.equals("")){
                filesWaitingRequestPool.sleepIfEmpty();
                int communicateToPort = 1;

                filename = listen(syncSocket);

                if (filesWaitingRequestPool.removeUpcomingFiles(filename)){
                    Thread t = new Thread(new SendFile(communicateToPort,ipAddress,filepath + filename));
                    t.start();
                }

            }
            System.out.println("ACABEI DE LER MEUS PUTOS");
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
            threadPool.inc_dec_nMaxFiles(1);
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
            System.out.println("Transfer file -> " + fileName + " ...");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(" Done Transfer file -> " + fileName + "!");
            threadPool.inc_dec_nMaxFiles(1);
            //condition.signalAll();
        }
    }

    public boolean doIRequest(boolean biggerNumber,boolean isSenderOrReceiver){
        if (biggerNumber) return isSenderOrReceiver;
        else return !isSenderOrReceiver;
    }

    public void processTransfers(Queue<TransferLogs> listOfTransfers,String filepath, DatagramSocket syncSocket,
                                 InetAddress ipAddress,int sendToPortHandler,boolean biggerNumber) {

        Thread listener = new Thread(new Listener(syncSocket,ipAddress,filepath));
        listener.start();
        int size = listOfTransfers.size();
        int max_files = threadPool.getNMaxFiles();
        System.out.println(max_files);

        while (size != 0 ){
            TransferLogs oneTransfer = listOfTransfers.poll();

            assert oneTransfer != null;
            if (doIRequest(biggerNumber,oneTransfer.isSenderOrReceiver())) {
                Thread t = new Thread(new ReceiveFile(sendToPortHandler,ipAddress,oneTransfer.getFileName()));
                t.start();
            }
            else filesWaitingRequestPool.addUpcomingFiles(oneTransfer.getFileName());

            threadPool.inc_dec_nMaxFiles(-1);
            size--;
            //System.out.println("size " + size);
            }


        while (threadPool.compareMaxFiles(max_files));
        filesWaitingRequestPool.setFinish();
        try {
            listener.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
