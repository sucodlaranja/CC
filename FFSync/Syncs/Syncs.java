package Syncs;

import java.net.InetAddress;
import java.util.*;

/**
 * goal: save all clients sync information.
 * (accessed by Listener.Listener and Handler)
 *
 * */
public class Syncs {

    private final Map<Integer, SyncHandler> syncs;

    public Syncs(){
        this.syncs = new HashMap<>();
    }

    /**
     * Create Syncs.SyncHandler
     * */
    public boolean createSync(String filepath, InetAddress ipAddress){
        // Create Sync instance.
        SyncHandler value = new SyncHandler(filepath, ipAddress);

        // Add Sync instance to pendingSyncs - if not already there.
        Integer key = value.getInfo().getId();
        if(!this.syncs.containsKey(key)){
            this.syncs.put(key, value);

            // Create and start thread.
            Thread syncHandlerThread = new Thread(value);
            syncHandlerThread.start();

            return true;
        }
        return false;
    }

    /**
     * Terminate one specific or all syncs.
     * */
    public void terminate(String id) {
        // TODO: Is it enough to close socket?

        if (!id.equals("all")){
            Integer key = Integer.parseInt(id);
            if(this.syncs.containsKey(key)){
                System.out.println("Terminating sync with id=" + key);
                this.syncs.get(key).closeSocket();

                this.syncs.remove(key);
            }
            else
                System.out.println("Invalid id. Try \"> help\"");
        }
        else{
            System.out.println("Terminating all syncs.");
            this.syncs.values().forEach(SyncHandler::closeSocket);
            this.syncs.clear();
        }

    }

    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        for(SyncHandler sync : this.syncs.values()){
            stringBuilder.append(sync.getInfo().toString());
        }
        return  stringBuilder.toString();
    }

}
