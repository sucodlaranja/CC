package Logs;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;



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

    // Construct logs from bytes
    public LogsManager(byte[] logBytes){
        filepath = "Non Defined";
        logs = new HashMap<>();

        // Formato: size@key1@value1@key2@value2@ (...)
        String logsStr = new String(logBytes, StandardCharsets.UTF_8);

        String[] logsStrArr = logsStr.split("@");
        int size = Integer.parseInt(logsStrArr[0]);

        for(int i = 1; i < size * 2; i+=2){
            String key = logsStrArr[i];
            FileTime value = FileTime.fromMillis(Long.parseLong(logsStrArr[i+1]));
            logs.put(key, value);
        }
    }

    // Serialize this class...
    public byte[] getBytes() {
        StringBuilder sb = new StringBuilder();
        sb.append(logs.size()).append("@");
        for (Map.Entry<String, FileTime> log : logs.entrySet())
            sb.append(log.getKey()).append("@").append(log.getValue().toMillis()).append("@");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }


    // Basic getLogs method
    public Map<String, FileTime> getLogs(){
        return new HashMap<>(logs);
    }

    // Updates all logs
    private boolean updateFileLogsAux(Path folder, String prePath) throws IOException {
        boolean update = false; // Was there a cache on the logs

        for(Path file: Files.list(folder).toList()) {

            // If the file is a directory call recursively
            if (Files.isDirectory(file)) updateFileLogsAux(file,prePath + file.getFileName().toString() + "/");

            // If not get the files info and insert on the map
            else {
                BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
                String filename = prePath + file.getFileName().toString();
                FileTime fl = attr.lastModifiedTime();

                // If the there is no file such in the logs or if it is not up-to-date
                if (!logs.containsKey(filename) || (fl.compareTo(logs.get(filename)) != 0)) {
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
    public Guide compareLogs(Map<String, FileTime> otherLogs){
        return new Guide(this.getLogs(), otherLogs);
    }

    // TODO: REMOVE LATER
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, FileTime> log : logs.entrySet())
            sb.append(log.getKey()).append("@").append(log.getValue().toMillis()).append("@");
        return sb.toString();
    }

}

