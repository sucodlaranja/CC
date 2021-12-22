package Syncs;

import java.net.InetAddress;
 /// (descricao breve se quiseres (recomendo))
 /**
  * (descricao)
  */
public class SyncInfo {
    /// "/home/rubensas/sync_folder" : without the final "/".
    private final String filepath;
    /// Name of the sync folder: "sync_folder", from the above example.          
    private final String filename;    
    /// IP Address of the other peer.
    private final InetAddress ipAddress;
    /// Sync ID: used to identify this sync.    
    private final int id;
    /// True if the sync is transfering files to another peer.                   
    private boolean active; 
    
     /// (descricao breve se quiseres)
    /**
     * (descricao)
     * @param filepath
     * @param ipAddress
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

    public String getFilepath() {
        return filepath;
    }
    public InetAddress getIpAddress() {
        return ipAddress;
    }
    public int getId() {
        return id;
    }
    public void activate() {
        this.active = true;
    }
    public void deactivate() {
        this.active = false;
    }
    public String getFilename(){
        return this.filename;
    }

    @Override
    public int hashCode(){
        return (this.getFilename() + this.ipAddress.toString()).hashCode();
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (o == null) return false;
        if (this.getClass() != o.getClass()) return false;
        SyncInfo sync = (SyncInfo) o;
        return this.filepath.equals(sync.getFilepath())
                && this.ipAddress.equals(sync.getIpAddress());
    }

    @Override
    public String toString(){
        return this.id + " - " +
                this.filepath + " - " +
                this.ipAddress.toString() + " - " +
                "active: " + this.active + "\n";
    }

    @Override
    public SyncInfo clone(){
        SyncInfo syncInfo = new SyncInfo(this.filepath, this.ipAddress);
        if(this.active)
            syncInfo.activate();

        return syncInfo;
    }
}
