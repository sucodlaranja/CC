import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) {
        // Parsing user input: "FFSync filepath ip"
        String filepath = "";
        InetAddress ip = null;
        if(args.length > 2){
            filepath = args[1];
            String ip_str = args[2];
            try{
                ip = InetAddress.getByName(ip_str);
            }
            catch (UnknownHostException e){
                System.out.println("Invalid ip.");
                e.printStackTrace();
            }
        }

        // Create Interpreter thread.
        Thread interpreter = new Thread(new Interpreter(filepath, ip));

        // Create Listener thread.
        Thread listener = new Thread(new Listener());

        // Run threads.
        interpreter.start();
        listener.start();

        // Wait for Interpreter to terminate.
        try {
            interpreter.join();
        }
        catch (InterruptedException e){
            System.out.println("Failed to join interpreter thread.");
            e.printStackTrace();
        }

        // TODO: HOW??
        // Close Listener


        System.out.println("Bye");
    }

}
