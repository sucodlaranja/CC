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

    private int nMaxFiles;
    private int allTransfers;
    private List<Triplet<String,Integer,Integer>> listOfTransfers;
    private Set<String> upcomingFiles;

    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    public static final int Waiting = 0;
    public static final int InProgress = 1;
    public static final int Completed = 2;

    public TransferHandler(List<Triplet<String,Integer,Integer>> listOfTransfers){
        nMaxFiles = 3;
        allTransfers = listOfTransfers.size();
        this.listOfTransfers = new ArrayList<>(listOfTransfers);
        upcomingFiles = new HashSet<>();
    }

    public class Listener implements Runnable{
        private int LISTENER_PORT;


        @Override
        public void run() {
            // está sempre a ouvir
        }
    }

    public class ReceiveFile implements Runnable{
        int SENDER_PORT;

        @Override
        public void run() {
            // esabelece concecção e recebe o file
        }
    }

    public class SendFile implements Runnable{
        @Override
        public void run() {
            // começa a enviar um file
        }
    }

    public void processTransfers() {

        Thread listener = new Thread(new Listener());
        listener.start();

        while (allTransfers > 0){
            while (nMaxFiles > 0){
                oneTransfer();
                allTransfers--;
                nMaxFiles--;
            }
            try {
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void oneTransfer(){
        for (Triplet<String,Integer,Integer> transfer : listOfTransfers){
            if (transfer.getThird() == Waiting){
                if (transfer.getSecond() == Logs.Sender) {
                    Thread request = new Thread(new ReceiveFile());
                    request.start();
                }
                else if (transfer.getSecond() == Logs.Receiver) upcomingFiles.add(transfer.getFirst());
                transfer.setThird(InProgress);
                return;
            }
        }
    }
}
