package HTTP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import HistoryRecorder.TransferHistory;

public class HTTPServer implements Runnable {

    private ServerSocket serverSocket;
    private int port;
    //TODO: meter para aqui o filepath

    public HTTPServer(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
    }
    
    public void run() {
        try {
            while(!serverSocket.isClosed()) {

                Socket clientSocket = serverSocket.accept();
            
                System.err.println("Client Connected");

                
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String s;
                String[] newString = null;
                while ((s = in.readLine()) != null) {
                    newString = s.split(" ");

                    if (newString[0] != null && newString[0].equals("GET")) {
                        break;
                    }
                    else if (s.isEmpty()) {
                        break;
                    }
                }

                OutputStream out = clientSocket.getOutputStream();
                out.write("HTTP/1.1 200 OK\r\n".getBytes());
                out.write("\r\n".getBytes());
                getHandler(newString[1],out);                                        //handler
                out.write("\r\n\r\n".getBytes());
                out.flush();
                System.err.println("Client connection closed!");
                out.close();
                in.close();
            }
        }
        catch (SocketException e){
            
            System.err.println("HTTP SERVER CLOSED"); // TODO: fazer qualquer coisa só pq sim...apenas deve haver 1 msg de terminação
        }
        //TODO: SEPARAR estas excessao?
        catch(IOException | ClassNotFoundException e) {
            System.out.println("Something's wrong, I can feel it");
        }

        // Termination message.
        System.err.println("HTTP server terminated.");
    }

    /**
     * TODO : ajeitar o filepath
     * @throws ClassNotFoundException
     */
    public void getHandler(String argument,OutputStream out) throws IOException, ClassNotFoundException {
        String [] splitArgument = argument.split("/");
        
        if(argument.equals("/")) {
            mainMenu(out);
        }
        else if(splitArgument.length == 2) {
            out.write(("<a href=\"http://localhost:" + port + "\">back</a>").getBytes());
            out.write((getSync("onomedofile")).getBytes());
        }
        
    }


    //gets all folders that are synchronizing
    public Set<String> getAllSyncs(String Filepath) throws IOException { 
        Path folder = Paths.get(Filepath);
        Set<String> files = new HashSet<>();
        for(Path file: Files.list(folder).toList()) {
            files.add(file.getFileName().toString());
            
        }
        return files;
    }

    //Gets all files information from a determined folder that is synchronizing
    public String getSync(String filename) throws IOException, ClassNotFoundException {
       
        TransferHistory history = new TransferHistory(filename);
        return history.toHTML();
      
    }

    /**
     *TODO: arranjar o filepath
     * 
     */
    public void mainMenu(OutputStream out) throws IOException {
        Set<String> files = getAllSyncs("HARDCODED");
        out.write("<b><h1>Menu Principal</h1></b>".getBytes());
        for( String entry : files) {
            out.write(("<p><a href=\"http://localhost:" + port + "/" + entry + "\">"+ entry  + "</a></p>").getBytes());
        }
    }

    // TODO: CONCURRENCY ???
    public void closeServer(){
        try{
            this.serverSocket.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}