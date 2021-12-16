package HistoryRecorder;

import java.io.*;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import Logs.TransferLogs;

public class TransferHistory implements Serializable {

    private final Map<String,FileTransferHistory> files;

    public TransferHistory(){
        files = new HashMap<>();
    }

    public TransferHistory(String filepath) throws IOException, ClassNotFoundException {
        ObjectInputStream is =
                new ObjectInputStream(new FileInputStream(filepath));
        TransferHistory transferHistory = (TransferHistory) is.readObject();

        this.files = new HashMap<>();
        for (Map.Entry<String, FileTransferHistory> file : transferHistory.files.entrySet()) {
            this.files.put(file.getKey(), file.getValue().clone());
        }
        is.close();
    }

    public void updateLogs(Set<String> fileNames){
        List<String> remove = new ArrayList<>();
        for (Map.Entry<String, FileTransferHistory> file : files.entrySet())
            if(!fileNames.remove(file.getKey())) remove.add(file.getKey());

        for(String file : remove) files.remove(file);

        for (String file : fileNames)
            files.put(file,new FileTransferHistory(null,-1,-1));
    }

    public void updateGuide(Queue<TransferLogs> guide){
        for(TransferLogs fileTransfer:guide)
            files.replace(fileTransfer.getFileName(),
                    new FileTransferHistory(FileTime.fromMillis(0),10,10));
    }

    public void saveTransferHistory(String filepath) throws IOException {
        ObjectOutputStream os =
                new ObjectOutputStream(new FileOutputStream(filepath));

        os.writeObject(this);
        os.flush();
        os.close();
    }

    public String toHTML(){

        StringBuilder html = new StringBuilder();

        for(HashMap.Entry<String,FileTransferHistory> entry: files.entrySet()) {
            html.append(entry.getValue().toHTML(entry.getKey()));
        }
        return html.toString();
    }

    public void printPOOO(){
        System.out.println("Tou aqui pra te dizer");
        for(HashMap.Entry<String,FileTransferHistory> entry: files.entrySet()) {

            System.out.println(entry.getKey() + " " );
        }
    }
    

}
