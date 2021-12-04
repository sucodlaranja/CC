import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**Isto e so a base do server 
 * meio burro so manda a cena em html 
 *  */ 
public class HTTPServer implements Runnable {

    ServerSocket serverSocket;
    int port;


    public HTTPServer(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
    }
    
    public void run() {
        Map<String,LogsManager> logs = new HashMap<>();
        logs.put("teste",null);
        logs.put("teste2",null);

        System.err.println("Server is listening on port " + port + ".");
        try {
        while(true) {
            
            Socket clientSocket = serverSocket.accept();
            System.err.println("Client Connected");
            
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String s;
            String [] newString = null;
                while( (s = in.readLine()) != null) {
                    newString = s.split(" ");
                
                    if(newString[0] != null && newString[0].equals("GET")) {
                        System.out.println(newString[1]);
                        break;
                    }
                    if(s.isEmpty()) {break;}
                }
                
            

                OutputStream out = clientSocket.getOutputStream();
                out.write("HTTP/1.1 200 OK\r\n".getBytes());
                out.write("\r\n".getBytes());
                

                // aqui deve estar um handler de getters
                getHandler(newString[1],logs,out);
                out.write("\r\n\r\n".getBytes());
                out.flush();
                System.err.println("Client connection closed!");
                out.close();
                in.close();
            }
        }

            catch(IOException e) {
                System.out.println("Something's wrong, I can feel it");
            }
        
    }

    public void getHandler(String argument,Map<String,LogsManager> syncs,OutputStream out) throws IOException {
        if(argument.equals("/")) {
            mainMenu(syncs,out);
        }
        else System.out.println(argument);
    }
    

    /**
     * Para fazer o menu principal, ns bem como é que vao organizar isto mas para motivos de simplicidade
     * @throws IOException
     */
    public void mainMenu(Map<String,LogsManager> syncs,OutputStream out) throws IOException {
        out.write("<b><h1>Menu Principal</h1></b>".getBytes());
        for( Map.Entry<String,LogsManager> entry :syncs.entrySet()) {
        out.write(("<p><a href=\"http://localhost:" + port + "/" + entry.getKey() + "\">"+ entry.getKey()  + "</a></p>").getBytes());
        }
    }



    public static void newdirectory(String filename,OutputStream out) throws IOException {
        out.write(("<a href=\"http://localhost:8081/\">back</a>").getBytes());
        out.write(("<p>teste nova directory</p>" + "<h1>" + "</h1>").getBytes());

    }
    public static void main(String[] args) {
        try {
            Thread server = new Thread(new HTTPServer(8081));
            server.start();
        } catch (IOException e) {
            
            e.printStackTrace();
        }
    }
}

