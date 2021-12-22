package HistoryRecorder;

import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Set;


import Logs.LogsRecord;
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

    public void updateLogs(Map<String, LogsRecord> logs ){
        List<String> remove = new ArrayList<>();
        for (Map.Entry<String, FileTransferHistory> file : files.entrySet()){
            LogsRecord fileTransferHistory;
            if( (fileTransferHistory = logs.remove(file.getKey())) != null) {
                long time = fileTransferHistory.fileTime().toMillis();
                if (time > file.getValue().getLastUpdated()) file.getValue().setLastUpdated(time);
            }
            else remove.add(file.getKey());
        }

        for(String file : remove)
            files.remove(file);

        for (Map.Entry<String, LogsRecord>file : logs.entrySet())
            files.put(file.getKey(),new FileTransferHistory(file.getValue().fileTime(), -1,-1));
    }

    public void updateGuide(Set<TransferLogs> transfers){
        for(TransferLogs fileTransfer:transfers) {
            FileTransferHistory fileTransferHistory;
            if ( (fileTransferHistory = files.get(fileTransfer.fileName())) != null) {
                fileTransferHistory.setTimeOfTransfer(fileTransfer.elapsedTime());
                fileTransferHistory.setBitsPSeg(fileTransfer.bitsPSeg());
            }
        }
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

    

}
