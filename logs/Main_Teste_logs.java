import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class Main_Teste_logs {
    public static void main(String[] args) {
        try {
            LogsManager l1 = new LogsManager("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\aa");
            l1.printLogs();
            System.out.println("------------------------------------");
            LogsManager l2 = new LogsManager("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\bb");
            l2.printLogs();

            TimeUnit.MINUTES.sleep(0);


            System.out.println("------------------------------------");
            Queue<TransferLogs> transferLogs = l1.compareLogs(l2.getLogs());
            l1.printTransfers(transferLogs);
            System.out.println("------------------------------------");

            InetAddress inetAddress = InetAddress.getByName("127.0.0.1");

            TransferHandler th = new TransferHandler(5);
            th.processTransfers(transferLogs,"hhh",new DatagramSocket(8888),inetAddress,55555,true);
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("end pls");

    }
}
