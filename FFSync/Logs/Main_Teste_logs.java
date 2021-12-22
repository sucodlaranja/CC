package Logs;

import HTTP.HTTPServer;
import HistoryRecorder.TransferHistory;

import java.io.File;
import java.io.IOException;


public class Main_Teste_logs {
    public static void main(String[] args) throws IOException {
        /*

        LogsManager l1 = new LogsManager("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\aa");
        LogsManager l2 = new LogsManager("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\bb");

        Map<String, LogsRecord> logs = l2.getLogs();

        long h = 2147483647;

        for(Map.Entry<String,LogsRecord> recordEntry : logs.entrySet())
            System.out.println(recordEntry.getKey() + " -> " + recordEntry.getValue().fileTime() + " " + (recordEntry.getValue().checksum()));

        Queue<TransferLogs> guide = l1.compareLogs(l2.getLogs()).getGuide();

        for(TransferLogs transferLogs : guide)
            System.out.println("-------" + transferLogs.fileName() + "         " + transferLogs.sender());





        String pasta = "HistorySaved";

        try {
            LogsManager l1 = new LogsManager("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\aa");
            LogsManager l2 = new LogsManager("C:\\Users\\jorge\\OneDrive\\Ambiente de Trabalho\\bb");

            TimeUnit.MINUTES.sleep(0);


            //InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
            //Transfers.TransferHandler th = new Transfers.TransferHandler(5);
            //th.processTransfers(transferLogs,"hhh",new DatagramSocket(8888),inetAddress,55555,true);

            TransferHistory s = new TransferHistory();
            s.updateLogs(l1.getLogs());
            s.saveTransferHistory(pasta + "\\ccc");

            TransferHistory h = new TransferHistory();
            h.updateLogs(l2.getLogs());
            h.saveTransferHistory(pasta + "\\aaa");


            Thread t = new Thread(new HTTPServer(8081));
            t.start();

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
        Files.list(Paths.get(pasta)).map(Path :: toFile).forEach(File :: delete);
        Files.delete(Paths.get(pasta));

        */

    }
}
