package Syncs;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/// Class that creates all the Syncs and holds all their information.
/**
 * This class creates new syncs and terminate old ones.
 * A reentrant lock is used to prevent concurrent access.
 */
public class Syncs {

    private final ReentrantLock lock; ///< Lock used to access the syncs map.
    private final Map<Integer, SyncHandler> syncs; ///< Holds all the existent syncs.

    public Syncs(){
        this.lock = new ReentrantLock();
        this.syncs = new HashMap<>();
    }

    /**
     * Creates a SyncHandler instance and runs it in its own thread.
     * This is the start of a sync.
     * @param filepath Filepath for the main sync folder.
     * @param ipAddress Other peer IPAdress.
     * @return true if the sync was created successfully.
     */
    public boolean createSync(String filepath, InetAddress ipAddress) {
        // Create Sync instance.
        SyncHandler value = new SyncHandler(filepath, ipAddress);

        // Add Sync instance to pendingSyncs - if not already there.
        Integer key = value.getInfo().getId();

        this.lock.lock();
        boolean returnVal = true;
        try {
            if (!this.syncs.containsKey(key)) {
                this.syncs.put(key, value);

                // Create and start thread.
                Thread syncHandlerThread = new Thread(value);
                syncHandlerThread.start();
            }
            else
                returnVal = false;
        }
        catch (Exception e){
            returnVal = false;
        }
        finally {
            this.lock.unlock();
        }
        return returnVal;
    }

    /**
     * Terminate one or all syncs, depending on the id given.
     * @param id If id.equals("all"), then the terminate method will terminate all syncs.
     *           Otherwise, it will terminate the sync with the given id, if exists.
     */
    public void terminate(String id) {
        this.lock.lock();
        try {
            if (!id.equals("all")) {
                Integer key = Integer.parseInt(id);
                if (this.syncs.containsKey(key)) {
                    System.out.println("Terminating sync with id=" + key);
                    this.syncs.get(key).closeSocket();
                    this.syncs.remove(key);
                }
                else
                    System.out.println("Invalid id. Try \"> help\"");
            }
            else {
                System.out.println("Terminating all syncs.");
                this.syncs.values().forEach(SyncHandler::closeSocket);
                this.syncs.clear();
            }
        }
        catch (Exception e){
            System.out.println("Failed sync termination: " + id);
        }
        finally {
            this.lock.unlock();
        }
    }

    /// Simple toString() method: puts the SyncInfo of all the Syncs in one String.
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        for(SyncHandler sync : this.syncs.values()){
            stringBuilder.append(sync.getInfo().toString());
        }
        return  stringBuilder.toString();
    }

}
