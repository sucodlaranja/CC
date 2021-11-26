import java.net.InetAddress;


public class Listener implements Runnable{



    public Listener(){

    }


    public static int checkPendingRequests(String filename, InetAddress peerAddress){
        // Check if there is any pending request with the respective filename and from the address given above.

        return -1;
    }

    @Override
    public void run() {
        /*
         * Listener goal: listen to UDP and TCP requests.
         * TCP requests: HTTP commands to retrieve client information.
         * UDP requests: Manage/Start FTRapid sync's.
         *
         * */

        // TODO: Receive (in UDP) requests with opcode INIT - create list of pending requests.



    }
}
