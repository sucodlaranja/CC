package HistoryRecorder;

import Logs.TransferLogs;

import java.io.*;
import java.nio.file.attribute.FileTime;
import java.util.*;

public class TransferHistory {

    private final Map<String,FileTransferHistory> files;

    public TransferHistory(){
        files = new HashMap<>();
    }

    public TransferHistory(String filepath) throws IOException, ClassNotFoundException {
        ObjectInputStream is =
                new ObjectInputStream(new FileInputStream(filepath));
        TransferHistory transferHistory = (TransferHistory) is.readObject();
        this.files = transferHistory.files;
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
        return "";
    }


}
