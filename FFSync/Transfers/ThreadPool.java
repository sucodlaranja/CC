package Transfers;
import Logs.TransferLogs;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class serves has the shared instances between the class Transfers.TransferHandler and the threads she creates.
 * When the number of threads gets to the max the condition is activated.
 * */
public class ThreadPool{

    /*
     number of maximum threads that the system creates
     this number will start at max and will decrease by each thread created
     and will increase by each thread that ends.
     */
    private int nMaxThreads;
    private Set<TransferLogs> completedTransfers;
    private final ReentrantLock lock;
    private final Condition condition;

    // Basic Constructor
    public ThreadPool(int nMaxThreads) {
        this.nMaxThreads = nMaxThreads;
        this.completedTransfers = new HashSet<>();
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
    }

    /*
     Compare the number of maxThreadsAllowed to the number of nMaxThreads (number of thread spaces still free)
     Waits until all threads finish
     */
    public void waitForAllThreadsToFinish(int maxThreadsAllowed) {
        lock.lock();
        try {
            while (nMaxThreads != maxThreadsAllowed)
                condition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void addTransferLogs(TransferLogs transferLogs){
        lock.lock();
        try {
           completedTransfers.add(transferLogs);
        }  finally {
            lock.unlock();
        }
    }

    public Set<TransferLogs> getTransferLogs(){
        lock.lock();
        try {
            return new HashSet<>(completedTransfers);
        }  finally {
            lock.unlock();
        }
    }

    // Simple get method
    public int getNMaxFiles(){
        lock.lock();
        try {
            return nMaxThreads;
        }
        finally {
            lock.unlock();
        }
    }

    /*
    Increases and decreases the number of max allowed threads
     */
    public void inc_dec_nMaxFiles(int incOrDec){
        lock.lock();
        try {
            nMaxThreads += incOrDec;

            //If the number of max threads increases we need to try to wake up the handler,
            // because there is space for more threads
            if (incOrDec == 1) condition.signalAll();

            //If the number of max threads is 0 the handler will go to sleep
            else if (incOrDec == -1 && nMaxThreads == 0) {
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
