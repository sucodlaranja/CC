import javax.imageio.IIOException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        try {
            Logs l1 = new Logs("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\aa");
            l1.printLogs();
            Logs l2 = new Logs("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\bb");


            TimeUnit.MINUTES.sleep(0);

            System.out.println("------------------------------------");
            l2.updateFileLogs();
            l2.printLogs();

            System.out.println("------------------------------------");
            l1.printTransfers(l1.compareLogs((l2.getLogs())));
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }
}
