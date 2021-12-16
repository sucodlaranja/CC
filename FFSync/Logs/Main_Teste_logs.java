package Logs;

import HTTP.HTTPServer;
import HistoryRecorder.TransferHistory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class Main_Teste_logs {
    public static void main(String[] args) throws IOException {
        String pasta = "HistorySaved";

        try {
            LogsManager l1 = new LogsManager("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\aa");
            LogsManager l2 = new LogsManager("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\bb");

            TimeUnit.MINUTES.sleep(0);


            Files.createDirectory(Paths.get(pasta));


            //InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
            //Transfers.TransferHandler th = new Transfers.TransferHandler(5);
            //th.processTransfers(transferLogs,"hhh",new DatagramSocket(8888),inetAddress,55555,true);

            TransferHistory s = new TransferHistory();
            s.updateLogs(l1.getFileNames());
            s.updateGuide(l1.compareLogs(l2.getLogs()).getGuide());
            s.saveTransferHistory(pasta + "\\ccc");

            TransferHistory h = new TransferHistory();
            h.updateLogs(l2.getFileNames());
            h.updateGuide(l2.compareLogs(l1.getLogs()).getGuide());
            h.saveTransferHistory(pasta + "\\aaa");


            //Thread t = new Thread(new HTTPServer(8081));
            //t.start();

        }
        catch (IOException | InterruptedException e) {
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
        Files.list(Paths.get(pasta)).map(Path :: toFile).forEach(File :: delete);
        Files.delete(Paths.get(pasta));

    }
}
