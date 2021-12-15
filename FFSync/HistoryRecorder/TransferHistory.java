package HistoryRecorder;

import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    public String toHTML(){}
    */

}
