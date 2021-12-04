package Transfers;

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

    private final ReentrantLock lock;
    private final Condition condition;

    // Basic Constructor
    public ThreadPool(int nMaxThreads) {
        this.nMaxThreads = nMaxThreads;
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
            while (nMaxThreads != maxThreadsAllowed) condition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
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
            System.out.println("nMaxFiles -> " + (nMaxThreads - incOrDec) + " -> " + nMaxThreads);

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
