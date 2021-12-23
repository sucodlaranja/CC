package Syncs;

import java.net.InetAddress;

 /**
  * Stores sync information such as the filename of the sync folder, the sync ID and its status.
  */
public class SyncInfo {

    private final String filepath; ///< "/home/rubensas/sync_folder" : without the final "/".
    private final String filename; ///< Name of the sync folder: "sync_folder", from the above example.
    private final InetAddress ipAddress; ///< IP Address of the other peer.
    private final int id; ///< Sync ID: used to identify this sync.
    private boolean active; ///< True if the sync is transfering files to another peer.
    
     /// Main constructor.
    /**
     * SyncInfo constructor takes the sync filepath and the peer IPAdress.
     * It stores information such as the filename of the sync folder, the sync ID and its status.
     * @param filepath Full filepath of sync folder.
     * @param ipAddress Peer address.
     */
    public SyncInfo(String filepath, InetAddress ipAddress){
        // "home/rubensas/sync_folder/" to "home/rubensas/sync_folder"
        this.filepath = filepath.endsWith("/") ? filepath.substring(0, filepath.length() - 1) : filepath;

        // filename = "sync_folder"
        String[] pathSplit = this.filepath.split("/");
        this.filename = pathSplit[pathSplit.length - 1];

        this.ipAddress = ipAddress;
        this.id = this.hashCode();
        this.active = false;
    }

    /// Filepath of sync is the full filepath to sync folder.
    public String getFilepath() {
        return filepath;
    }

    /// Get address of the other peer.
    public InetAddress getIpAddress() {
        return ipAddress;
    }

    /// Get id of sync.
    public int getId() {
        return id;
    }

    /// Set sync has active (actively transfering files).
    public void activate() {
        this.active = true;
    }

    /// Set sync has inactive (not actively transfering files).
    public void deactivate() {
        this.active = false;
    }

    /// Get filename of the sync (last name of the full path given).
    public String getFilename(){
        return this.filename;
    }

     /**
      * This hashcode method is used to generate a unique id to a sync.
      * Since the id does't need to be \b always different, we decided to use a combination of the filename and address of this sync.
      * @return Return "unique" identifier of this Sync.
      */
    public int hashCode(){
        return (this.getFilename() + this.ipAddress.toString()).hashCode();
    }

    /// Basic equals method.
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (this.getClass() != o.getClass()) return false;
        SyncInfo sync = (SyncInfo) o;
        return this.filepath.equals(sync.getFilepath())
                && this.ipAddress.equals(sync.getIpAddress());
    }

    /// Basic toString() method.
    public String toString(){
        return this.id + " - " +
                this.filepath + " - " +
                this.ipAddress.toString() + " - " +
                "active: " + this.active + "\n";
    }

    /// Basic clone method.
    public SyncInfo clone(){
        SyncInfo syncInfo = new SyncInfo(this.filepath, this.ipAddress);
        if(this.active)
            syncInfo.activate();

        return syncInfo;
    }
}
