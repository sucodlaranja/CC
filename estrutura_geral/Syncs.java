import java.net.InetAddress;
import java.util.*;

/**
 * goal: save all clients sync information.
 * (accessed by Listener and Handler)
 * how?
 *  lists of
 *      active syncs
 *      pending syncs
 *
 * */
public class Syncs {

    public class Sync {
        private final String filepath;
        private final InetAddress ipAddress;
        private final int id;
        private boolean active;

        public Sync(String filepath, InetAddress ipAddress){
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

        public boolean isActive() {
            return active;
        }
        public void activate() {
            this.active = true;
        }

        // TODO: Improve hashing mechanism.
        private int hashcode(){
            return (this.filepath + this.ipAddress.toString()).hashCode();
        }

        @Override
        public boolean equals(Object o){
            if (this == o) return true;
            if (o == null) return false;
            if (this.getClass() != o.getClass()) return false;
            Sync sync = (Sync) o;
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

    }

    private final Map<Integer, Sync> syncs;

    public Syncs(){
        this.syncs = new HashMap<>();
    }

    public boolean createSync(String filepath, InetAddress ipAddress){
        // Create Sync instance.
        Sync value = new Sync(filepath, ipAddress);

        // Add Sync instance to pendingSyncs.
        Integer key = value.getId();
        if(!this.syncs.containsKey(key)){
            this.syncs.put(key, value);
            return true;
        }
        return false;
    }

    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        for(Sync sync : this.syncs.values()){
            stringBuilder.append(sync.toString());
        }
        return  stringBuilder.toString();
    }

}
