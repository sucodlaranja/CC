package Transfers;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class serves has the shared instances between the class Transfers.TransferHandler and Listener.Listener it creates.
 * When there is no files in the set the listener sleeps.
 * */
public class FilesWaitingRequestPool{

    private boolean finish; // Can the listener end ?
    private final Set<String> upcomingFiles; // Set whit every filename that the listener will receive a request for.

    private final ReentrantLock lock;
    private final Condition condition;

    // Basic Constructor
    public FilesWaitingRequestPool() {
        finish = false;
        upcomingFiles = new HashSet<>();
        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    // Simple get method
    public boolean getFinish() {
        lock.lock();
        try {
            return finish;
        }
        finally {
            lock.unlock();
        }
    }

    // Change finish true and signals the listener to stop
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

    // If there is no file waiting for request listener sleeps
    public void sleepIfEmpty(){
        lock.lock();
        try {
            if (upcomingFiles.isEmpty())
                condition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    // Adds filename to set and wakes up the listener
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

    // Tries to remove a filename from the set and returns the result
    public boolean removeUpcomingFiles(String fileName){
        lock.lock();
        try {
            return upcomingFiles.remove(fileName);
        }
        finally {
            lock.unlock();
        }
    }

    // Returns the number files that are on the set
    public int size(){
        lock.lock();
        try {
            return upcomingFiles.size();
        }
        finally {
            lock.unlock();
        }
    }

}