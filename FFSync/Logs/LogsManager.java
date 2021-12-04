package Logs;

import Logs.TransferLogs;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents the logs of a given file,
 * and all the operations we can do to them
 * */
public class LogsManager {

    private final String filepath; // Path where the files sits
    private final Map<String, FileTime> logs; // Map with the files and timestamps

    // Basic Constructor
    public LogsManager(String filepath) throws IOException {
        this.filepath = filepath;
        logs = new HashMap<>();
        // adds all files logs to the class
        updateFileLogs();
    }

    // Basic getLogs method
    public Map<String, FileTime> getLogs(){
        return new HashMap<>(logs);
    }

    // Updates all logs
    private boolean updateFileLogsAux(Path folder, String prePath) throws IOException {
        boolean update = false; // Was there a cache on the logs

        for(Path file: Files.list(folder).collect(Collectors.toList())) {

            // If the file is a directory call recursively
            if (Files.isDirectory(file)) updateFileLogsAux(file,prePath + file.getFileName().toString() + "/");

            // If not get the files info and insert on the map
            else {
                BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
                String filename = prePath + file.getFileName().toString();
                FileTime fl = attr.lastModifiedTime();

                // If the there is no file such in the logs or if it is not up-to-date
                if (!logs.containsKey(filename) && (fl.compareTo(logs.get(filename)) != 0)) {
                    logs.put(filename, attr.lastModifiedTime());
                    update = true;
                }
            }
        }
        return update;
    }

    // Simple version update
    public boolean updateFileLogs() throws IOException {
        return this.updateFileLogsAux(Paths.get(filepath),"");
    }

    // Compare logs and generates the guide (queue) of transfers
    public Queue<TransferLogs> compareLogs(Map<String, FileTime> otherLogs){
        // Create guide
        Queue<TransferLogs> listOfTransfers = new LinkedList<>();

        // Compares all the files that I have on my logs
        for(Map.Entry<String, FileTime> file:  this.logs.entrySet()){
            // Compares his file and mine
            if (otherLogs.containsKey(file.getKey())){
                int comp = otherLogs.remove(file.getKey()).compareTo(file.getValue());

                if (comp > 0) listOfTransfers.add(new TransferLogs(file.getKey(), true));
                else if (comp < 0) listOfTransfers.add(new TransferLogs(file.getKey(), false));
            }
            // The other guy does not have this file
            else listOfTransfers.add(new TransferLogs(file.getKey(), false));
        }
        // Adds all the files that he has, and I don't.
        for(String fileName:  otherLogs.keySet()) listOfTransfers.add(new TransferLogs(fileName, true));

        return listOfTransfers;
    }



    // TODO DEBUG REMOVE
    public void printTransfers(Queue<TransferLogs> listOfTransfers){
        for (TransferLogs t: listOfTransfers){
            System.out.println( t.getFileName() + " " + t.isSenderOrReceiver());
        }
    }

    public void printLogs(){
        logs.forEach((key, value) -> System.out.println(key + " -> " + value.toString()));
    }
}

