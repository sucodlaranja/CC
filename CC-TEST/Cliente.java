import java.net.*;
import java.util.Scanner;
import java.io.*;
public class Cliente {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public Cliente(String address, int port) {
        try {
        socket = new Socket(address, port);
        System.out.println("Connected");

        in = new DataInputStream(System.in);
        out = new DataOutputStream(socket.getOutputStream());
        }
        catch(IOException e) {
            e.printStackTrace();
        }

        String line = ""; 
        while(!line.equals("Over")) {
            
            try {
                line = in.readLine();
                out.writeUTF(line);
            } catch (IOException e) {
                
                e.printStackTrace();
            }
        }
        try {
        in.close();
        out.close();
        socket.close();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Coloque o ip: ");
        String ip = scanner.nextLine();
        System.out.print("Coloque a porta: ");
        int port = scanner.nextInt();
        scanner.nextLine();
        scanner.close();

        Cliente cliente = new Cliente(ip, port);
    }
    
}