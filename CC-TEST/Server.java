import java.net.*;
import java.util.Scanner;
import java.io.*;

public class Server {
    private Socket socket;
    private ServerSocket server;
    private DataInputStream in;

    public Server(int port) {
        try {
            server = new ServerSocket(port);
            System.out.println("Server Listening on port: " + port);

            System.out.println("Waiting for a client ...");

            socket = server.accept();
            System.out.println("Cliente accepted");

            in = new DataInputStream(socket.getInputStream());
            String receive = "";

            while(!receive.equals("Over")) {
                receive = in.readUTF();
                System.out.println(receive);
            }


            System.out.println("Closing connection");
            socket.close();
            in.close();
        } catch (IOException e) {
            
            e.printStackTrace();
        }
        
    }
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Coloque a porta: ");
        int port = scanner.nextInt();
        scanner.nextLine();
        scanner.close();
        Server server = new Server(port);
    }
}
