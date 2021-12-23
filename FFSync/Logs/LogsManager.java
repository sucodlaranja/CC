package Logs;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

///This class is will show all logs of a folder.
/**
 * This class represents the logs of a given folder. \n
 * It will contain all the files of that folder.
 * */
public class LogsManager {

    /// Filepath where the folder is.
    private final String filepath;
    /// Map with the files and Record.
    private final Map<String, LogsRecord> logs;

    /// Basic Constructor.
    public LogsManager(String filepath) throws IOException {
        this.filepath = filepath;
        logs = new HashMap<>();
        // adds all files logs to the class
        updateFileLogs();
    }

    /// Construct logs from bytes.
    public LogsManager(byte[] logBytes){
        filepath = "Non Defined";
        logs = new HashMap<>();

        // Formato: size@key1@value1@key2@value2@ (...)
        String logsStr = new String(logBytes, StandardCharsets.UTF_8);

        String[] logsStrArr = logsStr.split("@");
        int size = Integer.parseInt(logsStrArr[0]);

        for(int i = 1; i < size * 3; i+=3){
            String key = logsStrArr[i];
            FileTime fileTime = FileTime.fromMillis(Long.parseLong(logsStrArr[i+1]));
            long checksum = Long.parseLong(logsStrArr[i+2]);
            logs.put(key, new LogsRecord(fileTime,checksum));
        }
    }

    /// Serialize this class for bytes.
    public byte[] getBytes() {
        StringBuilder sb = new StringBuilder();
        sb.append(logs.size()).append("@");
        for (Map.Entry<String, LogsRecord> log : logs.entrySet())
            sb.append(log.getKey()).append("@").append(log.getValue().fileTime().toMillis()).append("@")
                    .append(log.getValue().checksum()).append("@");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /// Basic getLogs method.
    public Map<String, LogsRecord> getLogs(){
        return new HashMap<>(logs);
    }

    /// Simple version update. Uses \ref updateFileLogsAux
    public void updateFileLogs() throws IOException {
        this.updateFileLogsAux(Paths.get(filepath), "");
    }

    /**
     * This is a private method.
     * This method will update all the logs of a folder.
     * Will run recursively for all the directories.
     *
     * @param folder The \b Path of the folder it needs to analyze.
     * @param prePath An auxiliar to the recursive call.
     * @throws IOException IO Exception.
     */
    public void updateFileLogsAux(Path folder, String prePath) throws IOException {

        for(Path file: Files.list(folder).toList()) {

            // If the file is a directory call recursively
            if (Files.isDirectory(file)) updateFileLogsAux(file,prePath + file.getFileName().toString() + "/");

            // If not get the files info and insert on the map
            else {
                BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
                String filename = prePath + file.getFileName().toString();
                LogsRecord logsRecord = new LogsRecord(attr.lastModifiedTime(),getCRC32Checksum(file.toString()));

                // If the there is no file such in the logs or if it is not up-to-date
                if (!logs.containsKey(filename) || (logsRecord.compareTo(logs.get(filename)) != 0)) {
                    logs.put(filename, logsRecord);
                }
            }
        }
    }

    /**
     * This is a private auxiliar method.
     * This method will create a long that is a checksum for a given file.
     * Will read the file in blocks and will update the checksum.
     *
     * @param fileName Name of the file we want to use.
     * @throws IOException IO Exception.
     */
    public long getCRC32Checksum(String fileName) throws IOException {
        Checksum crc32 = new CRC32();
        try (InputStream inputStream = new FileInputStream(fileName)) {
            byte[] bytes = new byte[1024];
            while (inputStream.read(bytes) != -1)
                crc32.update(bytes, 0, bytes.length);
        }
        return crc32.getValue();
    }

}

