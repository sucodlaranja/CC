package Logs;

import java.io.*;
import java.nio.file.attribute.FileTime;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

// TODO: ENCAPSULATE LOGSMANAGER STRUCTURES...WE DON'T NEED TO KNOW THAT THEY USE MAPS....
public class Guide {

    private final Queue<TransferLogs> guide;

    public Guide(Map<String, FileTime> alfa, Map<String, FileTime> beta) {
        this.guide = compareLogs(alfa, beta);
    }

    public Guide(byte[] guideBytes){
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(guideBytes);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

        Queue<TransferLogs> localGuide = new LinkedList<>();
        try {
            int size = dataInputStream.readInt();
            for(int i = 0; i < size; i++) {
                String filename = dataInputStream.readUTF();
                boolean isSender = dataInputStream.readBoolean();
                localGuide.add(new TransferLogs(filename, isSender));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            this.guide = localGuide;
        }
    }

    // Serialize this class.
    public byte[] getBytes(){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        try {
            dataOutputStream.writeInt(this.guide.size());
            for (TransferLogs transferLogs : this.guide) {
                dataOutputStream.writeUTF(transferLogs.getFileName());
                dataOutputStream.writeBoolean(transferLogs.isSenderOrReceiver());
            }
            dataOutputStream.flush();
        }
        catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return outputStream.toByteArray();
    }

    // Compare two LogsManager and generate guide.
    private Queue<TransferLogs> compareLogs(Map<String, FileTime> alfa, Map<String, FileTime> beta) {
        // Create guide
        Queue<TransferLogs> listOfTransfers = new LinkedList<>();

        // Compares all the files that I have on my logs
        for(Map.Entry<String, FileTime> file:  alfa.entrySet()){
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
