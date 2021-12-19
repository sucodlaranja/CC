package HTTP;

import java.io.*;
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

    private final ServerSocket serverSocket;
    private final int port;
    public static String HTTP_FILEPATH = "HistorySaved";

    public HTTPServer(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);

        if(Files.exists(Paths.get(HTTP_FILEPATH))){
            try {
                Files.list(Paths.get(HTTP_FILEPATH)).map(Path :: toFile).forEach(File :: delete);
                Files.delete(Paths.get(HTTP_FILEPATH));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Files.createDirectory(Paths.get(HTTP_FILEPATH));
    }
    
    public void run() {
        boolean closed = true;
            while(!serverSocket.isClosed() && closed) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String s;
                    String[] newString = null;
                    while ((s = in.readLine()) != null) {
                        newString = s.split(" ");

                        if (newString[0] != null && newString[0].equals("GET")) {
                            break;
                        }
                    }

                    OutputStream out = clientSocket.getOutputStream();
                    out.write("HTTP/1.1 200 OK\r\n".getBytes());
                    out.write("\r\n".getBytes());
                    if (s != null) getHandler(newString[1], out);                                        //handler
                    out.write("\r\n\r\n".getBytes());

                    out.flush();
                    out.close();
                    in.close();
                }
                catch (SocketException e){
                    System.err.println("HTTP SERVER CLOSED");
                    closed = false;
                }
                catch(IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
        }



        try {
            Files.list(Paths.get(HTTP_FILEPATH)).map(Path :: toFile).forEach(File :: delete);
            Files.delete(Paths.get(HTTP_FILEPATH));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Termination message.
        System.err.println("HTTP server terminated.");
    }

    /**
     *
     * @throws ClassNotFoundException
     */
    public void getHandler(String argument,OutputStream out) throws IOException, ClassNotFoundException {
        String [] splitArgument = argument.split("/");
        
        if(argument.equals("/") || argument.equals("/favicon.ico")) {
            mainMenu(out);
        }
        else if(splitArgument.length == 2) {
            out.write(("<h1>" + splitArgument[1] + "</h1>").getBytes());
            out.write(("<a href=\"http://localhost:" + port + "\">back</a>").getBytes());
            //out.write((getSync("onomedofile")).getBytes());
            out.write((getSync(HTTP_FILEPATH + "/" + splitArgument[1])).getBytes());
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
     *
     * 
     */
    public void mainMenu(OutputStream out) throws IOException {
        Set<String> files = getAllSyncs(HTTP_FILEPATH);
        out.write("<b><h1>Menu Principal</h1></b>".getBytes());
        for( String entry : files) {
            out.write(("<h2><a href=\"http://localhost:" + port + "/" + entry + "\">"+ entry  + "</a></h2>").getBytes());
        }
    }

    public void closeServer(){
        try{
            this.serverSocket.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}