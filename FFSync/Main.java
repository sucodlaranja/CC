import HTTP.HTTPServer;
import Listener.Listener;
import UI.Interpreter;
import java.io.IOException;
import java.net.*;

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
        assert l != null : "Invalid Listener.";
        Thread listener = new Thread(l);
        listener.start();

        // Create HTTP server.
        HTTPServer httpServer = null;
        try{
            httpServer = new HTTPServer(Listener.LISTENER_PORT);
        }
        catch (IOException e){
            e.printStackTrace();
        }

        // Start HTTP server.
        assert httpServer != null : "Invalid HTTP server.";
        Thread httpThread = new Thread(httpServer);
        httpThread.start();

        // Create and run UI.Interpreter thread.
        Thread interpreter = new Thread(new Interpreter(filepath, ip));
        interpreter.start();

        // Wait for UI.Interpreter to terminate.
        try {
            interpreter.join();
            System.err.println("Interpreter terminated.");
        }
        catch (InterruptedException e){
            System.out.println("Failed to join interpreter thread.");
            e.printStackTrace();
        }

        // Close Listener UDP and HTTP server.
        l.closeSocket();
        httpServer.closeServer();

        System.err.println("Main thread terminated.");
    }

}
