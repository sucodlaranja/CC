import Listener.Listener;
import UI.Interpreter;

import java.net.SocketException;

public class Main {

    public static void main(String[] args) {
        // Parsing user input: "FFSync filepath ip"
        String filepath = "";
        String ip = "";
        if(args.length > 2){
            filepath = args[1];
            ip = args[2];
        }

        // Create Listener.Listener thread.
        Listener l = null;
        try {
            l = new Listener();
        }
        catch (SocketException e){
            System.out.println("Failed to create listener.");
        }

        // Check if listener is not null, create thread and start thread.
        assert l != null : "Invalid Listener.Listener.";
        Thread listener = new Thread(l);
        listener.start();

        // Create and run UI.Interpreter thread.
        Thread interpreter = new Thread(new Interpreter(filepath, ip));
        interpreter.start();

        // Wait for UI.Interpreter to terminate.
        try {
            interpreter.join();
        }
        catch (InterruptedException e){
            System.out.println("Failed to join interpreter thread.");
            e.printStackTrace();
        }

        // TODO: IS IT ENOUGH?
        // Close Listener.Listener - can we do it here?
        l.closeSocket();

        System.out.println("Bye");
    }

}
