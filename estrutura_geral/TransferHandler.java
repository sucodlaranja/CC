import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handle the guide.
 *
 * */
public class TransferHandler {

    private int handlerPort;
    private int ip;
    private String path;
    private int nMaxFiles;
    private int allTransfers;
    private List<TransferLogs> listOfTransfers;
    private Set<String> upcomingFiles;

    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    public static final int Waiting = 0;
    public static final int InProgress = 1;
    public static final int Completed = 2;

    public TransferHandler(List<TransferLogs> listOfTransfers,String path,int handlerPort,int ip){
        this.handlerPort = handlerPort;
        this.ip = ip;
        nMaxFiles = 3;
        allTransfers = listOfTransfers.size();
        this.listOfTransfers = new ArrayList<>(listOfTransfers);
        upcomingFiles = new HashSet<>();
        this.path = path;
    }

    public class Listener implements Runnable{


        @Override
        public void run() {
            // está sempre a ouvir
        }
    }

    public class ReceiveFile implements Runnable{
        // esabelece concecção e recebe o file

        int listenerHandlerPort;
        String fileName;
        int ip;

        public ReceiveFile(int listenerHandlerPort,String fileName,int ip){
            this.listenerHandlerPort = listenerHandlerPort;
            this.fileName = fileName;
            this.ip = ip;
        }

        @Override
        public void run() {
            //Receiver_UDP c = new Receiver_UDP(fileName,listenerHandlerPort,ip);
            terminateTransfer(fileName);
            lock.lock();
            try {
                nMaxFiles++;
            }
            finally {
                lock.unlock();
            }
            condition.signal();
        }
    }

    public class SendFile implements Runnable{
        // começa a enviar um file

        int sendToPort;
        String fileName;
        int ip;

        public SendFile(int sendToPort,String fileName,int ip){
            this.sendToPort = sendToPort;
            this.fileName = fileName;
            this.ip = ip;
        }

        @Override
        public void run() {

            //Sender_UDP s = new Sender_UDP(path + filepath,portaEnvio,ip);
            terminateTransfer(fileName);
            lock.lock();
            try {
                nMaxFiles++;
                upcomingFiles.remove(fileName);
            }
            finally {
                lock.unlock();
            }
            condition.signal();
        }
    }

    private void terminateTransfer(String fileName){
        lock.lock();
        try {
            for (TransferLogs transfer : listOfTransfers){
                if (transfer.getFileName().equals(fileName) && transfer.getStateOfTransfer() == InProgress){
                    transfer.setStateOfTransfer(Completed);
                    break;
                }
            }
        }
        finally {
            lock.unlock();
        }
    }

    public void processTransfers() {

        Thread listener = new Thread(new Listener());
        listener.start();

        while (allTransfers > 0){

            lock.lock();
            try {
                while (nMaxFiles > 0){
                    oneTransfer();
                    allTransfers--;
                    nMaxFiles--;
                }
            }
            finally {
                lock.unlock();
            }

            try {
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private void oneTransfer(){
        lock.lock();
        try {
            for (TransferLogs transfer : listOfTransfers){
                if (transfer.getStateOfTransfer() == Waiting){
                    if (transfer.isSenderOrReceiver()) {
                        Thread request = new Thread(new ReceiveFile(handlerPort,transfer.getFileName(),ip));
                        request.start();
                    }
                    else upcomingFiles.add(transfer.getFileName());
                    transfer.setStateOfTransfer(InProgress);
                    break;
                }
            }
        }
        finally {
            lock.unlock();
        }
    }

}
