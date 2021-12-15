package HTTP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import Logs.LogsManager;

public class HTTPServer implements Runnable {

    private ServerSocket serverSocket;
    private int port;


    public HTTPServer(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
    }
    
    public void run() {
        Map<String,LogsManager> logs = new HashMap<>();
        logs.put("teste",null);
        logs.put("teste2",null);
        

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
                getHandler(newString[1], logs, out);                                        //handler
                out.write("\r\n\r\n".getBytes());
                out.flush();
                System.err.println("Client connection closed!");
                out.close();
                in.close();
            }
        }
        catch (SocketException e){
            
            logs.clear(); // TODO: fazer qualquer coisa só pq sim...apenas deve haver 1 msg de terminação
        }
        catch(IOException e) {
            System.out.println("Something's wrong, I can feel it");
        }

        // Termination message.
        System.err.println("HTTP server terminated.");
    }

    /**
     * TODO : Modificar isto para funcionar com os novos fileTransfers quando tiver material de teste
     */
    public void getHandler(String argument,Map<String,LogsManager> syncs,OutputStream out) throws IOException {
        String [] splitArgument = argument.split("/");
        
        if(argument.equals("/")) {
            mainMenu(syncs,out);
        }
        else if(splitArgument.length == 2) {
            for(Map.Entry<String,LogsManager> entry :syncs.entrySet()) {
                // TODO tem de ser direcionado para um metodo que mostra os logsmanagers 
                if(splitArgument[1].equals(entry.getKey())) {
                    out.write(("<b><h1>"+ entry.getKey() + "</h1></b>").getBytes());
                    out.write(("<h3>nome do file</h3>").getBytes());
                    out.write(("<p>&nbsp&nbsp&nbsp<i>"+ "Last Updated: </i>").getBytes());
                    out.write(("11/11/2922" + "</p>").getBytes());
                    out.write(("<p>&nbsp&nbsp&nbsp<i>"+ "Time of Transfer:20s </i>").getBytes());
                    out.write(("<p>&nbsp&nbsp&nbsp<i>"+ "Bits Per Second: 32</i>").getBytes());

                    out.write(("<h3>nome do file2</h3>").getBytes());
                    out.write(("<p>&nbsp&nbsp&nbsp<i>"+ "Last Updated: </i>").getBytes());
                    out.write(("11/11/2922" + "</p>").getBytes());
                    out.write(("<p>&nbsp&nbsp&nbsp<i>"+ "Time of Transfer: </i>").getBytes());
                    out.write(("<p>&nbsp&nbsp&nbsp<i>"+ "Bits Per Second: </i>").getBytes());
                    break;
                }
            }
        }
        else if(splitArgument.length == 3) {
            //TODO fazer dps de ter definido a cena, é colocar o metodo que vai deixar/parar de um file de sincronizar
            out.write(("<b><h1>OH maluco esta parte e dos logs que ainda nao ta feito</h1></b>").getBytes());
        }
    }
    

    /**
     *TODO: modificar isto para a nova maneira
     * @throws IOException
     */
    public void mainMenu(Map<String,LogsManager> syncs,OutputStream out) throws IOException {
        out.write("<b><h1>Menu Principal</h1></b>".getBytes());
        for( Map.Entry<String,LogsManager> entry :syncs.entrySet()) {
        out.write(("<p><a href=\"http://localhost:" + port + "/" + entry.getKey() + "\">"+ entry.getKey()  + "</a></p>").getBytes());
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