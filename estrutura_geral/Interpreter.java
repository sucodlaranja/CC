import java.net.InetAddress;

public class Interpreter implements Runnable{

    private final String firstCommandFilePath;
    private final InetAddress firstCommandIPAddress;

    public Interpreter(String filepath, InetAddress ip) {
        this.firstCommandFilePath = filepath;
        this.firstCommandIPAddress = ip;
    }

    @Override
    public void run() {
        /**
         * Interpreter goal: read user input
         * Possible types of user input:
         *  1. Start new sync.
         *  2. See list of all active syncs.
         *  3. Terminate specific sync.
         *  4. Terminate all sync's.
         *  5. Exit (includes step 4.).
         *
         * */







    }
}
