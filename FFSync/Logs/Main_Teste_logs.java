package Logs;

import HistoryRecorder.TransferHistory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class Main_Teste_logs {
    public static void main(String[] args) {

        try {
            LogsManager l1 = new LogsManager("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\aa");
            LogsManager l2 = new LogsManager("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\bb");

            TimeUnit.MINUTES.sleep(0);


            //InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
            //Transfers.TransferHandler th = new Transfers.TransferHandler(5);
            //th.processTransfers(transferLogs,"hhh",new DatagramSocket(8888),inetAddress,55555,true);

            TransferHistory h = new TransferHistory();
            h.saveTransferHistory("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\aaa");
            h.updateLogs(l1.getFileNames());
            h.updateGuide(l1.compareLogs(l2.getLogs()).getGuide());

            TransferHistory y = new TransferHistory("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\aaa");
        }
        catch (IOException | InterruptedException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("end pls");


        /*
        try {
            LogsManager l1 = new LogsManager("/home/rubensas/UM");
            System.out.println(l1);
            LogsManager l2 = new LogsManager(l1.getBytes());
            System.out.println(l2);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

         */

    }
}
