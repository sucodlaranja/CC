package Transfers;
import Logs.TransferLogs;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

///This class serves has the shared instances between the class TransferHandler and the threads she creates to send and receive files.
/**
 * This class maintains the order between all the threads that send and receive files.
 * It makes TransferHandler only use \ref nMaxThreads and stores every transfer information on the set.
 * */
public class ThreadPool{


    ///Number of maximum threads that the system creates
    private int nMaxThreads;
    /// Set with all the info of all transfers
    private final Set<TransferLogs> completedTransfers;
    /// Basic Reentrant Lock.
    private final ReentrantLock lock;
    /// Basic Condition.
    private final Condition condition;

    /// Basic Constructor
    public ThreadPool(int nMaxThreads) {
        this.nMaxThreads = nMaxThreads;
        this.completedTransfers = new HashSet<>();
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
    }

    /// Adds the information of a transfer to the set.
    public void addTransferLogs(TransferLogs transferLogs){
        lock.lock();
        try {
            completedTransfers.add(transferLogs);
        }  finally {
            lock.unlock();
        }
    }

    /// Basic get method.
    public Set<TransferLogs> getTransferLogs(){
        lock.lock();
        try {
            return new HashSet<>(completedTransfers);
        }  finally {
            lock.unlock();
        }
    }

    /// Simple get method
    public int getNMaxFiles(){
        lock.lock();
        try {
            return nMaxThreads;
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Compare the number of maxThreadsAllowed to the number of nMaxThreads (number of thread spaces still free). \n
     * Waits until all the number hits it´s maximum. It means that all threads created finish.
     * @param maxThreadsAllowed Number of maximum threads.
     */
    public void waitForAllThreadsToFinish(int maxThreadsAllowed) {
        lock.lock();
        try {
            while (nMaxThreads != maxThreadsAllowed)
                condition.await();
        } catch (InterruptedException ignored) {
        } finally {
            lock.unlock();
        }
    }


    /**
     * Increases and decreases the number of max allowed threads. \n
     * If the number decreases 0 then it makes \ref TransferHandler. \n
     * wait for a signal because there is no more threads available to transfer files. \n
     * If the number increases to 1, it means that the \ref TransferHandler is sleeping, and we need to wake it up. \n
     * , because there is room for more threads.
     *
     * @param incOrDec could be 1 or -1. Will determine what we will do.
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
                condition.await();
            }
        } catch (InterruptedException ignored) {
        } finally {
            lock.unlock();
        }
    }

}
