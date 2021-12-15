package HTTP;
import java.io.IOException;



public class MainTest {
    public static void main(String[] args) {
        try {
            Thread server = new Thread(new HTTPServer(8081));
            server.start();
        } catch (IOException e) {
            
            e.printStackTrace();
        }
    }
}
