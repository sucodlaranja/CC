package Logs;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/// This class represents a guide of transfers.

/**
 * This class represents a guide of transfers. \n
 * It will contain a queue that is to be followed by the \ref TransferHandler.
 */
public class Guide {

    /// Queue of transfers to be followed.
    private final Queue<TransferLogs> guide;

    /// This constructor will compare 2 \ref LogsManager and will generate the guide
    public Guide(Map<String, LogsRecord> alfa, Map<String, LogsRecord> beta) {
        this.guide = compareLogs(alfa, beta);
    }

    /// Construct guide from bytes.
    public Guide(byte[] guideBytes){
        this.guide = new LinkedList<>();

        // Formato: size@name1@bool1@name2@bool2@ (...)
        String logsStr = new String(guideBytes, StandardCharsets.UTF_8);
        String[] logsStrArr = logsStr.split("@");
        int size = Integer.parseInt(logsStrArr[0]);

        for(int i = 1; i < size * 4; i+=4){
            String name = logsStrArr[i];
            boolean isSender = Boolean.parseBoolean(logsStrArr[i+1]);
            long elapsedTime = Long.parseLong(logsStrArr[i+2]);
            double bitsPSeg = Double.parseDouble(logsStrArr[i+3]);
            this.guide.add(new TransferLogs(name, isSender,elapsedTime,bitsPSeg));
        }
    }

    /// Serialize this class.
    public byte[] getBytes(){
        StringBuilder sb = new StringBuilder();
        sb.append(this.guide.size()).append("@");
        for (TransferLogs transferLogs : this.guide)
            sb.append(transferLogs.fileName()).append("@")
                    .append(transferLogs.sender()).append("@")
                    .append(transferLogs.elapsedTime()).append("@")
                    .append(transferLogs.bitsPSeg()).append("@");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }


    /**
     * Compare two LogsManager and generate guide. \n
     * It will add all the possibilities of a transfer. \n
     * 1- alpha needs to actualize the file in beta \n
     * 2- beta needs to actualize the file in alpha \n
     * 3- alpha needs a new file in beta. \n
     * 4- beta needs a new file in alpha.
     *
     * @param alfa logs to compare.
     * @param beta others logs to compare.
     * @return The queue that it generates
     */
    public Queue<TransferLogs> compareLogs(Map<String, LogsRecord> alfa, Map<String, LogsRecord> beta) {
        // Create guide
        Queue<TransferLogs> listOfTransfers = new LinkedList<>();

        // Compares all the files that I have on my logs
        for(Map.Entry<String, LogsRecord> file:  alfa.entrySet()){
            // Compares his file and mine
            if (beta.containsKey(file.getKey())){
                int comp = beta.remove(file.getKey()).compareTo(file.getValue());

                if (comp > 0) listOfTransfers.add(
                        new TransferLogs(file.getKey(), true, -1,-1));
                else if (comp < 0)
                    listOfTransfers.add(new TransferLogs(file.getKey(), false,-1,-1));
            }
            // The other guy does not have this file
            else listOfTransfers.add(new TransferLogs(file.getKey(), false, -1,-1));
        }
        // Adds all the files that he has, and I don't.
        for(Map.Entry<String, LogsRecord> file:  beta.entrySet()) listOfTransfers.add(new TransferLogs(file.getKey(), true, -1,-1));

        return listOfTransfers;

    }

    /// Basic get.
    public Queue<TransferLogs> getGuide() {
        return guide;
    }

}
