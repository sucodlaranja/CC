package Logs;

import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.FileTime;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class Guide {

    private final Queue<TransferLogs> guide;

    public Guide(Map<String, LogsRecord> alfa, Map<String, LogsRecord> beta) {
        this.guide = compareLogs(alfa, beta);
    }

    public Guide(byte[] guideBytes){
        this.guide = new LinkedList<>();

        // Formato: size@name1@bool1@name2@bool2@ (...)
        String logsStr = new String(guideBytes, StandardCharsets.UTF_8);
        String[] logsStrArr = logsStr.split("@");
        int size = Integer.parseInt(logsStrArr[0]);

        for(int i = 1; i < size * 2; i+=2){
            String name = logsStrArr[i];
            boolean isSender = Boolean.parseBoolean(logsStrArr[i+1]);
            this.guide.add(new TransferLogs(name, isSender));
        }
    }

    // Serialize this class.
    public byte[] getBytes(){
        StringBuilder sb = new StringBuilder();
        sb.append(this.guide.size()).append("@");
        for (TransferLogs transferLogs : this.guide)
            sb.append(transferLogs.getFileName()).append("@").append(transferLogs.isSender()).append("@");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // Compare two LogsManager and generate guide.
    private Queue<TransferLogs> compareLogs(Map<String, LogsRecord> alfa, Map<String, LogsRecord> beta) {
        // Create guide
        Queue<TransferLogs> listOfTransfers = new LinkedList<>();

        // Compares all the files that I have on my logs
        for(Map.Entry<String, LogsRecord> file:  alfa.entrySet()){
            // Compares his file and mine
            if (beta.containsKey(file.getKey())){
                int comp = beta.remove(file.getKey()).compareTo(file.getValue());

                if (comp > 0) listOfTransfers.add(new TransferLogs(file.getKey(), true));
                else if (comp < 0) listOfTransfers.add(new TransferLogs(file.getKey(), false));
            }
            // The other guy does not have this file
            else listOfTransfers.add(new TransferLogs(file.getKey(), false));
        }
        // Adds all the files that he has, and I don't.
        for(String fileName:  beta.keySet()) listOfTransfers.add(new TransferLogs(fileName, true));

        return listOfTransfers;

    }

    // Get guide.
    public Queue<TransferLogs> getGuide() {
        return guide;
    }

}
