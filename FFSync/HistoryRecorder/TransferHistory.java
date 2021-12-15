package HistoryRecorder;

import java.util.HashMap;
import java.util.Map;

public class TransferHistory {

    private Map<String,FileTransferHistory> files;

    public TransferHistory(){
        files = new HashMap<>();
    }

    public TransferHistory(String filepath){

    }

    /*

    public updateLogs(Set<String> fileNames){
        //for (String file : fileNames) if (!files.containsKey(file)) files.put(file,new FileTransferHistory())
    }


    public saveTransferHistory(String filepath){

    }
    */

    public String toHTML(){
        StringBuilder html = new StringBuilder();
        for(HashMap.Entry<String,FileTransferHistory> entry: files.entrySet()) {
            html.append(entry.getValue().toHTML(entry.getKey()));
        }
        return html.toString();
    }
    

}
