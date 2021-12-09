package Logs;

import java.io.*;
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

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(logBytes);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

        try {
            int size = dataInputStream.readInt();
            for(int i = 0; i < size; i++) {
                String name = dataInputStream.readUTF();
                FileTime fileTime = FileTime.fromMillis(dataInputStream.readLong());
                logs.put(name, fileTime);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Serialize this class...
    public byte[] getBytes() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        try {
            dataOutputStream.writeInt(logs.size());
            for (Map.Entry<String, FileTime> log : logs.entrySet()) {
                dataOutputStream.writeUTF(log.getKey());
                dataOutputStream.writeLong(log.getValue().toMillis());
            }
            dataOutputStream.flush();
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return outputStream.toByteArray();
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

