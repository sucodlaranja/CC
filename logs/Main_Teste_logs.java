import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Main_Teste_logs {
    public static void main(String[] args) {
        try {
            LogsManager l1 = new LogsManager("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\aa");
            l1.printLogs();
            LogsManager l2 = new LogsManager("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\bb");


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
