import java.net.InetAddress;


public class SyncInfo {
    private final String filepath;
    private final InetAddress ipAddress;
    private final int id;
    private boolean active;

    public SyncInfo(String filepath, InetAddress ipAddress){
        this.filepath = filepath;
        this.ipAddress = ipAddress;
        this.id = this.hashcode();
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

    private int hashcode(){
        return (this.filepath + this.ipAddress.toString()).hashCode();
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
