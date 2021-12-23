package Main;

import HTTP.HTTPServer;
import Listener.Listener;
import UI.Interpreter;
import java.io.IOException;
import java.net.*;

/// Main class is used to start all the main components of the FFSync program.
public class Main {

    /**
     * Main class is used to start all the main components of the FFSync program.
     * Listener, HTTP server and the interpreter are all started here.
     * A Sync can be started using command line arguments.
     * @param args Arguments used to start a Sync: "FFSync /home/rubensas/tp2-folder3 192.168.1.108".
     *             We can give the program more than one address.
     */
    public static void main(String[] args) {
        // Parsing user input: "FFSync filepath ip1 ip2 ipn"
        String filepath = "";
        String[] ips = null;
        if(args.length > 2){
            filepath = args[1];
            ips = new String[args.length - 2]; // take the FFSync and filename.
            System.arraycopy(args, 2, ips, 0, ips.length);
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
        assert l != null : "Invalid Listener.";
        Thread listener = new Thread(l);
        listener.start();

        // Create HTTP server.
        HTTPServer httpServer = null;
        try{
            httpServer = new HTTPServer(Listener.LISTENER_PORT);
        }
        catch (IOException ignored){
        }

        // Start HTTP server.
        assert httpServer != null : "Invalid HTTP server.";
        Thread httpThread = new Thread(httpServer);
        httpThread.start();

        // Create and run UI.Interpreter thread.
        Thread interpreter = new Thread(new Interpreter(filepath, ips));
        interpreter.start();

        // Wait for UI.Interpreter to terminate.
        try {
            interpreter.join();
            System.err.println("Interpreter terminated.");
        }
        catch (InterruptedException e){
            System.out.println("Failed to join interpreter thread.");
        }

        // Close Listener UDP and HTTP server.
        l.closeSocket();
        httpServer.closeServer();

        System.err.println("Main thread terminated.");
    }

}
