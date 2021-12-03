import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**Isto e so a base do server 
 * meio burro so manda a cena em html 
 *  */ 
public class HTTPServer {
    public static void main(String[] args) throws IOException {
        int port = 8081;
        
        ServerSocket serverSocket = new ServerSocket(port);
        System.err.println("Server is listening on port " + port + ".");
        while(true) {
            
            Socket clientSocket = serverSocket.accept();
            System.err.println("Client Connected");
            
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String s;
            String [] newString = null;
            try {
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
                out.write("<b><h1>Teste titulo</h1></b>".getBytes());
                if(newString != null && newString[1].equals("/?Carlos")) {
                    newdirectory("tetetetetete",out);
                }
                else {
                out.write("<b><h1>Teste titulo</h1></b>".getBytes());
                out.write(("<p>teste normal</p>" + "<h1>" + "</h1>").getBytes());
                doLink("Carlos",out);
                }
                out.write("\r\n\r\n".getBytes());
                out.flush();
                System.err.println("Client connection closed!");
                out.close();
                in.close();
            }
            catch(IOException e) {
                System.out.println("Something's wrong, I can feel it");
            }
        }
    }
    // funcao que faz um link a partir de um nome, para ser usado nos fsyncs para desligar/ligar sincronizacoes
    public static void doLink(String filename,OutputStream out) throws IOException{
        
        out.write(("<a href=\"http://localhost:8081/"+ filename + "\">teste link</a>").getBytes());
        
      
    
    }
    public static void newdirectory(String filename,OutputStream out) throws IOException {
        out.write(("<a href=\"http://localhost:8081/\">back</a>").getBytes());
        out.write(("<p>teste nova directory</p>" + "<h1>" + "</h1>").getBytes());

    }
}

