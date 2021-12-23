package Transfers;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/// This class contains the request we expect in the \ref TransferHandler.
/**
 * This class serves has the shared instances between the class \ref TransferHandler and \ref TransferHandler.Listener.\n
 * This class will be used by various threads, so it needs locks and condition. \n
 * The condition will make threads sleep when there is no need to be working.
 * */
public class FilesWaitingRequestPool{

    /// Will be true where there is no more requests to be listened to.
    private boolean finish;
    /// Set whit every filename that the listener will receive a request for.
    private final Set<String> upcomingFiles;
    /// Basic Reentrant Lock.
    private final ReentrantLock lock;
    /// Basic Condition.
    private final Condition condition;

    /// Basic Constructor
    public FilesWaitingRequestPool() {
        finish = false;
        upcomingFiles = new HashSet<>();
        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    /// Simple get method whit locks
    public boolean getFinish() {
        lock.lock();
        try {
            return finish;
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * This method will set the instance \b finish to true to make the listener. \n
     * Also wakes \b Listener up if he is asleep.
     */
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


    /// If there is no request pending make the listener sleep.
    public void sleepIfEmpty(){
        lock.lock();
        try {
            if (upcomingFiles.isEmpty()) condition.await();
        } catch (InterruptedException ignored) {
        } finally {
            lock.unlock();
        }
    }

    /// Adds filename to set and wakes up the listener if it is sleeping.
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

    /// Tries to remove a filename from the set and returns the result
    public boolean removeUpcomingFiles(String fileName){
        lock.lock();
        try {
            return upcomingFiles.remove(fileName);
        }
        finally {
            lock.unlock();
        }
    }

}